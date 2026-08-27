package com.lozaine.resourceworldresetter.reset;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.config.EvacuationSettings;
import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.config.PluginSettings;
import com.lozaine.resourceworldresetter.config.RegenerationSettings;
import com.lozaine.resourceworldresetter.config.ResetPolicySettings;
import com.lozaine.resourceworldresetter.config.ScheduleSettings;
import com.lozaine.resourceworldresetter.config.ScheduleType;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.config.WorldOperationalState;
import com.lozaine.resourceworldresetter.history.InterruptedOperationMarker;
import com.lozaine.resourceworldresetter.history.ResetHistoryEntry;
import com.lozaine.resourceworldresetter.history.ResetJournal;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.multiverse.RegenerationFailureReason;
import com.lozaine.resourceworldresetter.multiverse.RegenerationOutcome;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRequest;
import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import com.lozaine.resourceworldresetter.world.SafeLocation;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResetCoordinatorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void cancellablePreEventStopsBeforeJournalEvacuationAndMultiverse() throws Exception {
        FakeGateway gateway = new FakeGateway();
        FakeEvacuation evacuation = new FakeEvacuation();
        RecordingEvents events = new RecordingEvents(false);
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, evacuation, events);

        ResetOutcome outcome = coordinator.reset("resource_id");

        assertThat(outcome.failure()).isEqualTo(ResetFailureType.EVENT_CANCELLED);
        assertThat(gateway.regenerationCalls).hasValue(0);
        assertThat(coordinator.recentHistory(10)).isEmpty();
        assertThat(events.after).isSameAs(outcome);
    }

    @Test
    void neverCallsMultiverseWhilePlayersRemain() throws Exception {
        FakeGateway gateway = new FakeGateway();
        FakeEvacuation evacuation = new FakeEvacuation();
        evacuation.remaining = OptionalInt.of(1);
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, evacuation);

        ResetOutcome outcome = coordinator.reset("resource_id");

        assertThat(outcome.failure()).isEqualTo(ResetFailureType.PLAYERS_REMAINING);
        assertThat(outcome.safety()).isEqualTo(FailureSafety.SAFE_TO_RETRY);
        assertThat(gateway.regenerationCalls).hasValue(0);
        assertThat(coordinator.recentHistory(10)).singleElement()
                .extracting(ResetHistoryEntry::failure)
                .isEqualTo(ResetFailureType.PLAYERS_REMAINING);
    }

    @Test
    void missingWorldFailsBeforeLifecycleAndIsPersisted() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.worlds.remove("resource");
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, new FakeEvacuation());

        ResetOutcome outcome = coordinator.reset("resource_id");

        assertThat(outcome.failure()).isEqualTo(ResetFailureType.WORLD_NOT_REGISTERED);
        assertThat(outcome.safety()).isEqualTo(FailureSafety.SAFE_TO_RETRY);
        assertThat(gateway.regenerationCalls).hasValue(0);
        assertThat(coordinator.recentHistory(1)).singleElement()
                .extracting(ResetHistoryEntry::failure)
                .isEqualTo(ResetFailureType.WORLD_NOT_REGISTERED);
    }

    @Test
    void perWorldLockRejectsAConcurrentResetAndBlocksIncomingRwrTeleports() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.pauseRegeneration = true;
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, new FakeEvacuation());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<ResetOutcome> first = executor.submit(() -> coordinator.reset("resource_id"));
            assertThat(gateway.regenerationEntered.await(5, TimeUnit.SECONDS)).isTrue();

            assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isTrue();
            ResetOutcome second = coordinator.reset("resource_id");
            assertThat(second.failure()).isEqualTo(ResetFailureType.WORLD_BUSY);
            assertThat(gateway.regenerationCalls).hasValue(1);

            gateway.releaseRegeneration.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).successful()).isTrue();
            assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isFalse();
        } finally {
            gateway.releaseRegeneration.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void globalCollisionGuardRejectsAnotherWorldDuringHeavyReset() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.worlds.put("second", snapshot("second", "second"));
        gateway.pauseRegeneration = true;
        ResetCoordinator coordinator = coordinator(settings("resource", "second"), gateway, new FakeEvacuation());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<ResetOutcome> first = executor.submit(() -> coordinator.reset("resource_id"));
            assertThat(gateway.regenerationEntered.await(5, TimeUnit.SECONDS)).isTrue();

            ResetOutcome second = coordinator.reset("second_id");
            assertThat(second.failure()).isEqualTo(ResetFailureType.GLOBAL_RESET_BUSY);

            gateway.releaseRegeneration.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS).successful()).isTrue();
        } finally {
            gateway.releaseRegeneration.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void asynchronousRegenerationKeepsLocksUntilThePlatformFutureCompletes() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.asyncOutcome = new CompletableFuture<>();
        RecordingEvents events = new RecordingEvents(true);
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, new FakeEvacuation(), events);

        CompletionStage<ResetOutcome> reset = coordinator.resetAsync("resource_id");

        assertThat(reset.toCompletableFuture()).isNotDone();
        assertThat(coordinator.status("resource_id").phase()).isEqualTo(ResetPhase.REGENERATE);
        assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isTrue();
        assertThat(coordinator.resetAsync("resource_id").toCompletableFuture().get().failure())
                .isEqualTo(ResetFailureType.WORLD_BUSY);
        assertThat(events.after).isNull();

        gateway.asyncOutcome.complete(new RegenerationOutcome.Success(gateway.worlds.get("resource")));
        ResetOutcome outcome = reset.toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(outcome.successful()).isTrue();
        assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isFalse();
        assertThat(events.after).isSameAs(outcome);
    }

    @Test
    void asynchronousEvacuationDoesNotBlockAndRegenerationWaitsForCompletion() throws Exception {
        FakeGateway gateway = new FakeGateway();
        FakeEvacuation evacuation = new FakeEvacuation();
        evacuation.asyncResult = new CompletableFuture<>();
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, evacuation);

        CompletionStage<ResetOutcome> reset = coordinator.resetAsync("resource_id");

        assertThat(reset.toCompletableFuture()).isNotDone();
        assertThat(coordinator.status("resource_id").phase()).isEqualTo(ResetPhase.EVACUATE);
        assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isTrue();
        assertThat(gateway.regenerationCalls).hasValue(0);

        evacuation.asyncResult.complete(new EvacuationResult.Success(1));
        ResetOutcome outcome = reset.toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(outcome.successful()).isTrue();
        assertThat(gateway.regenerationCalls).hasValue(1);
        assertThat(coordinator.blocksIncomingRwrTeleport("resource")).isFalse();
    }

    @Test
    void multiverseFailureIsAmbiguousAndVisibleInStatusAndHistory() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.outcome = new RegenerationOutcome.Failed(
                RegenerationFailureReason.CREATE_FAILED,
                "CREATE_FAILED",
                "Multiverse could not recreate the world.");
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, new FakeEvacuation());

        ResetOutcome outcome = coordinator.reset("resource_id");

        assertThat(outcome.failure()).isEqualTo(ResetFailureType.MULTIVERSE_CREATE_FAILED);
        assertThat(outcome.safety()).isEqualTo(FailureSafety.AMBIGUOUS_REVIEW_REQUIRED);
        assertThat(coordinator.status("resource_id").phase()).isEqualTo(ResetPhase.FAILED);
        assertThat(coordinator.recentHistory(1)).singleElement().satisfies(entry -> {
            assertThat(entry.failure()).isEqualTo(ResetFailureType.MULTIVERSE_CREATE_FAILED);
            assertThat(entry.safety()).isEqualTo(FailureSafety.AMBIGUOUS_REVIEW_REQUIRED);
        });
    }

    @Test
    void verificationRejectsChangedMultiverseIdentity() throws Exception {
        FakeGateway gateway = new FakeGateway();
        gateway.identityAfterRegeneration = "unexpected";
        ResetCoordinator coordinator = coordinator(settings("resource"), gateway, new FakeEvacuation());

        ResetOutcome outcome = coordinator.reset("resource_id");

        assertThat(outcome.failure()).isEqualTo(ResetFailureType.WORLD_IDENTITY_CHANGED);
        assertThat(outcome.safety()).isEqualTo(FailureSafety.AMBIGUOUS_REVIEW_REQUIRED);
    }

    @Test
    void startupRecordsInterruptedRegenerationWithoutRepeatingIt() throws Exception {
        ResetJournal firstJournal = new ResetJournal(temporaryDirectory, 100, CLOCK);
        firstJournal.mark(new InterruptedOperationMarker(
                "operation-1",
                "resource_id",
                "resource",
                "resource",
                ResetPhase.REGENERATE,
                "2026-08-26T23:59:00Z",
                "2026-08-26T23:59:30Z"));
        FakeGateway gateway = new FakeGateway();
        ResetCoordinator restarted = new ResetCoordinator(
                () -> settings("resource"),
                gateway,
                new FakeEvacuation(),
                new ResetJournal(temporaryDirectory, 100, CLOCK),
                CLOCK,
                Logger.getLogger("ResetCoordinatorTest"));

        List<ResetHistoryEntry> recovered = restarted.recoverInterruptedOperations();

        assertThat(recovered).singleElement().satisfies(entry -> {
            assertThat(entry.terminalPhase()).isEqualTo(ResetPhase.INTERRUPTED);
            assertThat(entry.safety()).isEqualTo(FailureSafety.AMBIGUOUS_REVIEW_REQUIRED);
        });
        assertThat(gateway.regenerationCalls).hasValue(0);
        assertThat(new ResetJournal(temporaryDirectory, 100, CLOCK).activeMarkers()).isEmpty();
    }

    private ResetCoordinator coordinator(
            PluginSettings settings,
            FakeGateway gateway,
            PlayerEvacuationService evacuation)
            throws Exception {
        return new ResetCoordinator(
                () -> settings,
                gateway,
                evacuation,
                new ResetJournal(temporaryDirectory, 100, CLOCK),
                CLOCK,
                Logger.getLogger("ResetCoordinatorTest"));
    }

    private ResetCoordinator coordinator(
            PluginSettings settings,
            FakeGateway gateway,
            PlayerEvacuationService evacuation,
            ResetEventPublisher events)
            throws Exception {
        return new ResetCoordinator(
                () -> settings,
                gateway,
                evacuation,
                new ResetJournal(temporaryDirectory, 100, CLOCK),
                CLOCK,
                Logger.getLogger("ResetCoordinatorTest"),
                events);
    }

    private static PluginSettings settings(String... worldNames) {
        Map<String, ManagedWorldSettings> worlds = new java.util.LinkedHashMap<>();
        for (String worldName : worldNames) {
            String id = worldName + "_id";
            worlds.put(
                    id,
                    new ManagedWorldSettings(
                            id,
                            worldName,
                            worldName,
                            true,
                            true,
                            new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                            List.of(60, 10),
                            new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                            new EvacuationSettings(true, "world"),
                            WorldOperationalState.MANAGED));
        }
        return new PluginSettings(
                5,
                ZoneId.of("UTC"),
                "world",
                new ResetPolicySettings(2, 30, true),
                worlds,
                new TeleportSettings(true, false, true, Map.of()));
    }

    private static WorldSnapshot snapshot(String name, String key) {
        return new WorldSnapshot(
                key,
                name,
                name,
                true,
                "NORMAL",
                42L,
                "",
                "",
                "NORMAL",
                true,
                true,
                name + " 0 64 0");
    }

    private static final class FakeEvacuation implements PlayerEvacuationService {
        private EvacuationResult result = new EvacuationResult.Success(0);
        private OptionalInt remaining = OptionalInt.of(0);
        private CompletableFuture<EvacuationResult> asyncResult;

        @Override
        public EvacuationResult evacuate(String sourceWorld, EvacuationSettings settings) {
            return result;
        }

        @Override
        public CompletionStage<EvacuationResult> evacuateAsync(
                String sourceWorld, EvacuationSettings settings) {
            return asyncResult == null ? PlayerEvacuationService.super.evacuateAsync(sourceWorld, settings) : asyncResult;
        }

        @Override
        public OptionalInt remainingPlayers(String sourceWorld) {
            return remaining;
        }
    }

    private static final class RecordingEvents implements ResetEventPublisher {
        private final boolean proceed;
        private ResetOutcome after;

        private RecordingEvents(boolean proceed) {
            this.proceed = proceed;
        }

        @Override
        public boolean beforeReset(ManagedWorldSettings world, String operationId) {
            return proceed;
        }

        @Override
        public void afterReset(ResetOutcome outcome) {
            after = outcome;
        }
    }

    private static final class FakeGateway implements WorldProvider {
        private final Map<String, WorldSnapshot> worlds = new java.util.concurrent.ConcurrentHashMap<>();
        private final AtomicInteger regenerationCalls = new AtomicInteger();
        private final CountDownLatch regenerationEntered = new CountDownLatch(1);
        private final CountDownLatch releaseRegeneration = new CountDownLatch(1);
        private volatile boolean pauseRegeneration;
        private volatile String identityAfterRegeneration;
        private volatile RegenerationOutcome outcome;
        private volatile CompletableFuture<RegenerationOutcome> asyncOutcome;

        private FakeGateway() {
            worlds.put("resource", snapshot("resource", "resource"));
        }

        @Override
        public List<WorldSnapshot> registeredWorlds() {
            return List.copyOf(worlds.values());
        }

        @Override
        public List<WorldSnapshot> loadedWorlds() {
            return registeredWorlds();
        }

        @Override
        public Optional<WorldSnapshot> world(String name) {
            return Optional.ofNullable(worlds.get(name));
        }

        @Override
        public DestinationResult resolveSafeDestination(String name) {
            return new DestinationResult.Available(new SafeLocation(name, 0, 64, 0), false);
        }

        @Override
        public RegenerationOutcome regenerate(RegenerationRequest request) {
            regenerationCalls.incrementAndGet();
            regenerationEntered.countDown();
            if (pauseRegeneration) {
                try {
                    if (!releaseRegeneration.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test reset was not released");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("test reset interrupted", exception);
                }
            }
            if (outcome != null) {
                return outcome;
            }
            if (identityAfterRegeneration != null) {
                worlds.put(request.worldName(), snapshot(request.worldName(), identityAfterRegeneration));
            }
            return new RegenerationOutcome.Success(worlds.get(request.worldName()));
        }

        @Override
        public CompletionStage<RegenerationOutcome> regenerateAsync(RegenerationRequest request) {
            if (asyncOutcome == null) {
                return WorldProvider.super.regenerateAsync(request);
            }
            regenerationCalls.incrementAndGet();
            regenerationEntered.countDown();
            return asyncOutcome;
        }

        @Override
        public Set<String> registeredWorldNames() {
            return Set.copyOf(worlds.keySet());
        }

        @Override
        public String defaultWorldName() {
            return "world";
        }
    }
}
