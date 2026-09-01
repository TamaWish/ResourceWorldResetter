package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.history.InterruptedOperationMarker;
import io.github.tamawish.rwr.history.ResetHistoryEntry;
import io.github.tamawish.rwr.history.ResetJournal;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.multiverse.RegenerationFailureReason;
import io.github.tamawish.rwr.multiverse.RegenerationOutcome;
import io.github.tamawish.rwr.multiverse.RegenerationRequest;
import io.github.tamawish.rwr.multiverse.WorldSnapshot;
import io.github.tamawish.rwr.world.WorldProvider;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ResetCoordinator implements ResetExecutor, ResetAccessPolicy {
    private final Supplier<PluginSettings> settings;
    private final WorldProvider gateway;
    private final PlayerEvacuationService evacuation;
    private final ResetJournal journal;
    private final Clock clock;
    private final Logger logger;
    private final ResetEventPublisher events;
    private final ConcurrentHashMap<String, AtomicBoolean> worldLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ResetStatus> statuses = new ConcurrentHashMap<>();
    private final AtomicBoolean heavyResetActive = new AtomicBoolean();

    public ResetCoordinator(
            Supplier<PluginSettings> settings,
            WorldProvider gateway,
            PlayerEvacuationService evacuation,
            ResetJournal journal,
            Clock clock,
            Logger logger) {
        this(settings, gateway, evacuation, journal, clock, logger, ResetEventPublisher.NONE);
    }

    public ResetCoordinator(
            Supplier<PluginSettings> settings,
            WorldProvider gateway,
            PlayerEvacuationService evacuation,
            ResetJournal journal,
            Clock clock,
            Logger logger,
            ResetEventPublisher events) {
        this.settings = settings;
        this.gateway = gateway;
        this.evacuation = evacuation;
        this.journal = journal;
        this.clock = clock;
        this.logger = logger;
        this.events = events;
    }

    public List<ResetHistoryEntry> recoverInterruptedOperations() throws IOException {
        List<ResetHistoryEntry> recovered = journal.recoverInterrupted();
        for (ResetHistoryEntry entry : recovered) {
            statuses.put(
                    normalize(entry.worldId()),
                    new ResetStatus(
                            entry.worldId(),
                            entry.multiverseWorld(),
                            ResetPhase.INTERRUPTED,
                            entry.operationId(),
                            entry.message()));
            logger.warning("[RWR] [WARN] Reset " + entry.operationId() + " for " + entry.worldId()
                    + " was interrupted; no automatic regeneration was attempted.");
        }
        return recovered;
    }

    @Override
    public ResetOutcome reset(String worldId) {
        ManagedWorldSettings world = settings.get().world(worldId).orElse(null);
        if (world == null) {
            return ResetOutcome.rejected(
                    worldId,
                    "unknown",
                    ResetFailureType.UNKNOWN_WORLD_ID,
                    FailureSafety.NOT_RETRYABLE,
                    "No configured v5 world ID named '" + worldId + "'.");
        }
        if (!world.canReset()) {
            return ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.WORLD_NOT_MANAGED,
                    FailureSafety.NOT_RETRYABLE,
                    "World is " + world.state() + " and cannot enter the reset coordinator.");
        }

        String lockKey = normalize(world.id());
        AtomicBoolean worldLock = worldLocks.computeIfAbsent(lockKey, ignored -> new AtomicBoolean());
        if (!worldLock.compareAndSet(false, true)) {
            return ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.WORLD_BUSY,
                    FailureSafety.SAFE_TO_RETRY,
                    "A reset is already active for this world.");
        }
        if (!heavyResetActive.compareAndSet(false, true)) {
            worldLock.set(false);
            return ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.GLOBAL_RESET_BUSY,
                    FailureSafety.SAFE_TO_RETRY,
                    "Another heavy reset is active; simultaneous regeneration is blocked.");
        }

        try {
            String operationId = UUID.randomUUID().toString();
            if (!events.beforeReset(world, operationId)) {
                ResetOutcome cancelled = new ResetOutcome(
                        operationId,
                        world.id(),
                        world.multiverseWorld(),
                        ResetPhase.FAILED,
                        ResetFailureType.EVENT_CANCELLED,
                        FailureSafety.SAFE_TO_RETRY,
                        "Reset was cancelled by a ResourceWorldPreResetEvent listener.");
                events.afterReset(cancelled);
                return cancelled;
            }
            ResetOutcome outcome = execute(world, operationId);
            events.afterReset(outcome);
            return outcome;
        } finally {
            heavyResetActive.set(false);
            worldLock.set(false);
        }
    }

    @Override
    public CompletionStage<ResetOutcome> resetAsync(String worldId) {
        ManagedWorldSettings world = settings.get().world(worldId).orElse(null);
        if (world == null) {
            return completed(ResetOutcome.rejected(
                    worldId,
                    "unknown",
                    ResetFailureType.UNKNOWN_WORLD_ID,
                    FailureSafety.NOT_RETRYABLE,
                    "No configured v5 world ID named '" + worldId + "'."));
        }
        if (!world.canReset()) {
            return completed(ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.WORLD_NOT_MANAGED,
                    FailureSafety.NOT_RETRYABLE,
                    "World is " + world.state() + " and cannot enter the reset coordinator."));
        }

        String lockKey = normalize(world.id());
        AtomicBoolean worldLock = worldLocks.computeIfAbsent(lockKey, ignored -> new AtomicBoolean());
        if (!worldLock.compareAndSet(false, true)) {
            return completed(ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.WORLD_BUSY,
                    FailureSafety.SAFE_TO_RETRY,
                    "A reset is already active for this world."));
        }
        if (!heavyResetActive.compareAndSet(false, true)) {
            worldLock.set(false);
            return completed(ResetOutcome.rejected(
                    world.id(),
                    world.multiverseWorld(),
                    ResetFailureType.GLOBAL_RESET_BUSY,
                    FailureSafety.SAFE_TO_RETRY,
                    "Another heavy reset is active; simultaneous regeneration is blocked."));
        }

        String operationId = UUID.randomUUID().toString();
        try {
            if (!beforeResetSafely(world, operationId)) {
                ResetOutcome cancelled = new ResetOutcome(
                        operationId,
                        world.id(),
                        world.multiverseWorld(),
                        ResetPhase.FAILED,
                        ResetFailureType.EVENT_CANCELLED,
                        FailureSafety.SAFE_TO_RETRY,
                        "Reset was cancelled by a ResourceWorldPreResetEvent listener.");
                afterResetSafely(cancelled);
                heavyResetActive.set(false);
                worldLock.set(false);
                return completed(cancelled);
            }

            CompletionStage<ResetOutcome> execution = executeAsync(world, operationId);
            return execution.handle((outcome, error) -> {
                if (error == null) {
                    return outcome;
                }
                Throwable cause = rootCause(error);
                return unjournaledFailure(
                        world,
                        operationId,
                        ResetFailureType.MULTIVERSE_API_EXCEPTION,
                        FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                        "Asynchronous reset execution failed: "
                                + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }).whenComplete((outcome, error) -> {
                try {
                    afterResetSafely(outcome);
                } finally {
                    heavyResetActive.set(false);
                    worldLock.set(false);
                }
            });
        } catch (RuntimeException exception) {
            heavyResetActive.set(false);
            worldLock.set(false);
            throw exception;
        }
    }

    public ResetStatus status(String worldId) {
        ManagedWorldSettings world = settings.get().world(worldId).orElse(null);
        if (world == null) {
            return ResetStatus.idle(worldId, "unknown");
        }
        return statuses.getOrDefault(normalize(world.id()), ResetStatus.idle(world.id(), world.multiverseWorld()));
    }

    public List<ResetHistoryEntry> recentHistory(int count) {
        return journal.recent(count);
    }

    @Override
    public boolean blocksIncomingRwrTeleport(String multiverseWorld) {
        return statuses.values().stream()
                .anyMatch(status -> status.multiverseWorld().equalsIgnoreCase(multiverseWorld)
                        && status.phase().blocksIncomingRwrTeleports());
    }

    private ResetOutcome execute(ManagedWorldSettings configured, String operationId) {
        String startedAt = clock.instant().toString();
        setStatus(configured, operationId, ResetPhase.PRECHECK, "Validating " + provider() + " world state.");

        InterruptedOperationMarker marker = new InterruptedOperationMarker(
                operationId,
                configured.id(),
                configured.multiverseWorld(),
                "",
                ResetPhase.PRECHECK,
                startedAt,
                startedAt);
        try {
            journal.mark(marker);
        } catch (IOException exception) {
            return unjournaledFailure(
                    configured,
                    operationId,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.SAFE_TO_RETRY,
                    "Reset aborted before preflight because its recovery marker could not be persisted: "
                            + exception.getMessage());
        }

        Optional<WorldSnapshot> beforeOption;
        try {
            beforeOption = gateway.world(configured.multiverseWorld());
        } catch (RuntimeException exception) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.MULTIVERSE_API_EXCEPTION,
                    FailureSafety.SAFE_TO_RETRY,
                    provider() + " preflight lookup failed before regeneration: "
                            + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
        if (beforeOption.isEmpty()) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.WORLD_NOT_REGISTERED,
                    FailureSafety.SAFE_TO_RETRY,
                    "The configured world is no longer registered in " + provider() + ".");
        }
        WorldSnapshot before = beforeOption.get();
        if (!before.loaded()) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.WORLD_NOT_LOADED,
                    FailureSafety.SAFE_TO_RETRY,
                    "The configured " + provider() + " world is not loaded.");
        }

        try {
            marker = marker.withExpectedWorldIdentity(before.identity(), clock.instant().toString());
            journal.mark(marker);
        } catch (IOException exception) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.SAFE_TO_RETRY,
                    "Reset aborted before evacuation because its identity marker could not be persisted: "
                            + exception.getMessage());
        }

        boolean lifecycleMayHaveStarted = false;
        try {
            marker = transition(configured, marker, ResetPhase.EVACUATE, "Evacuating players.");
            EvacuationResult evacuationResult = evacuation.evacuate(
                    configured.multiverseWorld(), configured.evacuation());
            if (evacuationResult instanceof EvacuationResult.Failed failed) {
                return terminalFailure(
                        configured,
                        marker,
                        failed.reason(),
                        FailureSafety.SAFE_TO_RETRY,
                        failed.message());
            }

            OptionalInt remainingPlayers = evacuation.remainingPlayers(configured.multiverseWorld());
            if (remainingPlayers.isEmpty()) {
                return terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.WORLD_NOT_LOADED,
                        FailureSafety.SAFE_TO_RETRY,
                        "The world unloaded during evacuation.");
            }
            if (remainingPlayers.getAsInt() > 0) {
                return terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.PLAYERS_REMAINING,
                        FailureSafety.SAFE_TO_RETRY,
                        remainingPlayers.getAsInt() + " player(s) remain; " + provider() + " was not called.");
            }

            marker = transition(
                    configured,
                    marker,
                    ResetPhase.REGENERATE,
                    "Calling the authoritative " + provider() + " regeneration operation.");
            lifecycleMayHaveStarted = true;
            RegenerationOutcome regeneration = gateway.regenerate(request(configured));
            if (regeneration instanceof RegenerationOutcome.Rejected rejected) {
                lifecycleMayHaveStarted = false;
                return terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.MULTIVERSE_REJECTED,
                        FailureSafety.SAFE_TO_RETRY,
                        rejected.reason() + ": " + rejected.message());
            }
            if (regeneration instanceof RegenerationOutcome.Failed failed) {
                return terminalFailure(
                        configured,
                        marker,
                        mapFailure(failed.reason()),
                        FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                        failed.upstreamReason() + ": " + failed.message());
            }

            marker = transition(
                    configured,
                    marker,
                    ResetPhase.VERIFY,
                    "Verifying registry, loaded state, identity, and safe spawn.");
            ResetOutcome verification = verify(configured, marker, before.identity());
            if (verification != null) {
                return verification;
            }
            return terminalSuccess(configured, marker);
        } catch (IOException exception) {
            FailureSafety safety = lifecycleMayHaveStarted
                    ? FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
                    : FailureSafety.SAFE_TO_RETRY;
            return unjournaledFailure(
                    configured,
                    operationId,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    safety,
                    "Reset stopped because operation state could not be persisted: " + exception.getMessage());
        } catch (RuntimeException exception) {
            FailureSafety safety = lifecycleMayHaveStarted
                    ? FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
                    : FailureSafety.SAFE_TO_RETRY;
            ResetFailureType failure = lifecycleMayHaveStarted
                    ? ResetFailureType.MULTIVERSE_API_EXCEPTION
                    : marker.phase() == ResetPhase.EVACUATE
                            ? ResetFailureType.EVACUATION_FAILED
                            : ResetFailureType.VERIFICATION_FAILED;
            return terminalFailure(
                    configured,
                    marker,
                    failure,
                    safety,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private CompletionStage<ResetOutcome> executeAsync(ManagedWorldSettings configured, String operationId) {
        String startedAt = clock.instant().toString();
        setStatus(configured, operationId, ResetPhase.PRECHECK, "Validating " + provider() + " world state.");

        InterruptedOperationMarker marker = new InterruptedOperationMarker(
                operationId,
                configured.id(),
                configured.multiverseWorld(),
                "",
                ResetPhase.PRECHECK,
                startedAt,
                startedAt);
        try {
            journal.mark(marker);
        } catch (IOException exception) {
            return completed(unjournaledFailure(
                    configured,
                    operationId,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.SAFE_TO_RETRY,
                    "Reset aborted before preflight because its recovery marker could not be persisted: "
                            + exception.getMessage()));
        }

        Optional<WorldSnapshot> beforeOption;
        try {
            beforeOption = gateway.world(configured.multiverseWorld());
        } catch (RuntimeException exception) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.MULTIVERSE_API_EXCEPTION,
                    FailureSafety.SAFE_TO_RETRY,
                    provider() + " preflight lookup failed before regeneration: "
                            + exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }
        if (beforeOption.isEmpty()) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.WORLD_NOT_REGISTERED,
                    FailureSafety.SAFE_TO_RETRY,
                    "The configured world is no longer registered in " + provider() + "."));
        }
        WorldSnapshot before = beforeOption.get();
        if (!before.loaded()) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.WORLD_NOT_LOADED,
                    FailureSafety.SAFE_TO_RETRY,
                    "The configured " + provider() + " world is not loaded."));
        }

        InterruptedOperationMarker evacuationMarker;
        try {
            marker = marker.withExpectedWorldIdentity(before.identity(), clock.instant().toString());
            journal.mark(marker);
            evacuationMarker = transition(configured, marker, ResetPhase.EVACUATE, "Evacuating players.");
        } catch (IOException exception) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.SAFE_TO_RETRY,
                    "Reset stopped because operation state could not be persisted: " + exception.getMessage()));
        } catch (RuntimeException exception) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.EVACUATION_FAILED,
                    FailureSafety.SAFE_TO_RETRY,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }

        CompletionStage<EvacuationResult> evacuationStage;
        try {
            evacuationStage = evacuation.evacuateAsync(
                    configured.multiverseWorld(), configured.evacuation());
        } catch (RuntimeException exception) {
            return completed(terminalFailure(
                    configured,
                    evacuationMarker,
                    ResetFailureType.EVACUATION_FAILED,
                    FailureSafety.SAFE_TO_RETRY,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }

        return evacuationStage
                .handle((result, error) -> {
                    if (error == null) {
                        return result;
                    }
                    Throwable cause = rootCause(error);
                    return new EvacuationResult.Failed(
                            ResetFailureType.EVACUATION_FAILED,
                            0,
                            cause.getClass().getSimpleName() + ": " + cause.getMessage());
                })
                .thenCompose(result -> continueAfterEvacuation(
                        configured, evacuationMarker, before.identity(), result));
    }

    private CompletionStage<ResetOutcome> continueAfterEvacuation(
            ManagedWorldSettings configured,
            InterruptedOperationMarker marker,
            String expectedWorldIdentity,
            EvacuationResult evacuationResult) {
        if (evacuationResult instanceof EvacuationResult.Failed failed) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    failed.reason(),
                    FailureSafety.SAFE_TO_RETRY,
                    failed.message()));
        }

        try {
            OptionalInt remainingPlayers = evacuation.remainingPlayers(configured.multiverseWorld());
            if (remainingPlayers.isEmpty()) {
                return completed(terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.WORLD_NOT_LOADED,
                        FailureSafety.SAFE_TO_RETRY,
                        "The world unloaded during evacuation."));
            }
            if (remainingPlayers.getAsInt() > 0) {
                return completed(terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.PLAYERS_REMAINING,
                        FailureSafety.SAFE_TO_RETRY,
                        remainingPlayers.getAsInt() + " player(s) remain; " + provider() + " was not called."));
            }

            InterruptedOperationMarker regenerationMarker = transition(
                    configured,
                    marker,
                    ResetPhase.REGENERATE,
                    "Calling the authoritative " + provider() + " regeneration operation.");
            CompletionStage<RegenerationOutcome> regeneration = gateway.regenerateAsync(request(configured));
            return regeneration.handle((outcome, error) -> {
                if (error != null) {
                    Throwable cause = rootCause(error);
                    return terminalFailure(
                            configured,
                            regenerationMarker,
                            ResetFailureType.MULTIVERSE_API_EXCEPTION,
                            FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                            cause.getClass().getSimpleName() + ": " + cause.getMessage());
                }
                return completeRegeneration(configured, regenerationMarker, expectedWorldIdentity, outcome);
            });
        } catch (IOException exception) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.SAFE_TO_RETRY,
                    "Reset stopped because operation state could not be persisted: " + exception.getMessage()));
        } catch (RuntimeException exception) {
            return completed(terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.EVACUATION_FAILED,
                    FailureSafety.SAFE_TO_RETRY,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage()));
        }
    }

    private ResetOutcome completeRegeneration(
            ManagedWorldSettings configured,
            InterruptedOperationMarker marker,
            String expectedWorldIdentity,
            RegenerationOutcome regeneration) {
        try {
            if (regeneration instanceof RegenerationOutcome.Rejected rejected) {
                return terminalFailure(
                        configured,
                        marker,
                        ResetFailureType.MULTIVERSE_REJECTED,
                        FailureSafety.SAFE_TO_RETRY,
                        rejected.reason() + ": " + rejected.message());
            }
            if (regeneration instanceof RegenerationOutcome.Failed failed) {
                return terminalFailure(
                        configured,
                        marker,
                        mapFailure(failed.reason()),
                        FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                        failed.upstreamReason() + ": " + failed.message());
            }

            marker = transition(
                    configured,
                    marker,
                    ResetPhase.VERIFY,
                    "Verifying registry, loaded state, identity, and safe spawn.");
            ResetOutcome verification = verify(configured, marker, expectedWorldIdentity);
            return verification == null ? terminalSuccess(configured, marker) : verification;
        } catch (IOException exception) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.JOURNAL_UNAVAILABLE,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    "Reset stopped because operation state could not be persisted: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.MULTIVERSE_API_EXCEPTION,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private ResetOutcome verify(
            ManagedWorldSettings configured,
            InterruptedOperationMarker marker,
            String expectedWorldIdentity) {
        Optional<WorldSnapshot> currentOption = gateway.world(configured.multiverseWorld());
        if (currentOption.isEmpty()) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.VERIFICATION_FAILED,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    "Regeneration returned success but the world is not registered.");
        }
        WorldSnapshot current = currentOption.get();
        if (!current.loaded()) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.VERIFICATION_FAILED,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    "Regeneration returned success but the world is not loaded.");
        }
        if (!expectedWorldIdentity.equalsIgnoreCase(current.identity())) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.WORLD_IDENTITY_CHANGED,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    "Expected " + provider() + " world " + expectedWorldIdentity + " but found " + current.identity()
                            + '.');
        }
        DestinationResult spawn = gateway.resolveSafeDestination(configured.multiverseWorld());
        if (spawn instanceof DestinationResult.Unavailable unavailable) {
            return terminalFailure(
                    configured,
                    marker,
                    ResetFailureType.VERIFICATION_FAILED,
                    FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
                    "World spawn verification failed: " + unavailable.reason() + ": " + unavailable.message());
        }
        return null;
    }

    private InterruptedOperationMarker transition(
            ManagedWorldSettings world,
            InterruptedOperationMarker marker,
            ResetPhase phase,
            String message)
            throws IOException {
        InterruptedOperationMarker updated = marker.withPhase(phase, clock.instant().toString());
        journal.mark(updated);
        setStatus(world, marker.operationId(), phase, message);
        return updated;
    }

    private ResetOutcome terminalSuccess(
            ManagedWorldSettings world,
            InterruptedOperationMarker marker) {
        ResetOutcome outcome = new ResetOutcome(
                marker.operationId(),
                world.id(),
                world.multiverseWorld(),
                ResetPhase.COMPLETE,
                null,
                FailureSafety.NOT_RETRYABLE,
                provider() + " regeneration completed and independent verification passed.");
        persistTerminal(marker, outcome);
        setStatus(world, marker.operationId(), ResetPhase.COMPLETE, outcome.message());
        logger.info("[RWR] [INFO] " + world.displayName() + " reset completed successfully.");
        return outcome;
    }

    private ResetOutcome terminalFailure(
            ManagedWorldSettings world,
            InterruptedOperationMarker marker,
            ResetFailureType failure,
            FailureSafety safety,
            String message) {
        ResetOutcome outcome = new ResetOutcome(
                marker.operationId(),
                world.id(),
                world.multiverseWorld(),
                ResetPhase.FAILED,
                failure,
                safety,
                message);
        persistTerminal(marker, outcome);
        setStatus(world, marker.operationId(), ResetPhase.FAILED, failure + ": " + message);
        logger.warning("[RWR] [WARN] " + world.displayName() + " reset failed [" + failure + ", " + safety
                + "]: " + message);
        return outcome;
    }

    private ResetOutcome unjournaledFailure(
            ManagedWorldSettings world,
            String operationId,
            ResetFailureType failure,
            FailureSafety safety,
            String message) {
        ResetOutcome outcome = new ResetOutcome(
                operationId,
                world.id(),
                world.multiverseWorld(),
                ResetPhase.FAILED,
                failure,
                safety,
                message);
        setStatus(world, operationId, ResetPhase.FAILED, failure + ": " + message);
        logger.warning("[RWR] [WARN] " + world.displayName() + " reset failed [" + failure + "]: " + message);
        return outcome;
    }

    private void persistTerminal(InterruptedOperationMarker marker, ResetOutcome outcome) {
        try {
            journal.complete(new ResetHistoryEntry(
                    marker.operationId(),
                    marker.worldId(),
                    marker.multiverseWorld(),
                    marker.startedAt(),
                    clock.instant().toString(),
                    outcome.phase(),
                    outcome.failure(),
                    outcome.safety(),
                    outcome.message()));
        } catch (IOException exception) {
            logger.log(
                    Level.SEVERE,
                    "Could not persist terminal history for reset " + marker.operationId()
                            + "; its interrupted marker is retained for recovery.",
                    exception);
        }
    }

    private void setStatus(
            ManagedWorldSettings world,
            String operationId,
            ResetPhase phase,
            String message) {
        statuses.put(
                normalize(world.id()),
                new ResetStatus(world.id(), world.multiverseWorld(), phase, operationId, message));
    }

    private static RegenerationRequest request(ManagedWorldSettings world) {
        RegenerationSettings settings = world.regeneration();
        return new RegenerationRequest(
                world.multiverseWorld(),
                settings.seedPolicy(),
                settings.fixedSeed(),
                settings.keepWorldConfig(),
                settings.keepGameRules(),
                settings.keepWorldBorder());
    }

    private static ResetFailureType mapFailure(RegenerationFailureReason reason) {
        return switch (reason) {
            case DELETE_FAILED -> ResetFailureType.MULTIVERSE_DELETE_FAILED;
            case CREATE_FAILED -> ResetFailureType.MULTIVERSE_CREATE_FAILED;
            case API_EXCEPTION -> ResetFailureType.MULTIVERSE_API_EXCEPTION;
        };
    }

    private static <T> CompletionStage<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private boolean beforeResetSafely(ManagedWorldSettings world, String operationId) {
        try {
            return events.beforeReset(world, operationId);
        } catch (RuntimeException | LinkageError error) {
            logger.log(Level.SEVERE, "Pre-reset event publication failed; cancelling reset " + operationId, error);
            return false;
        }
    }

    private void afterResetSafely(ResetOutcome outcome) {
        try {
            events.afterReset(outcome);
        } catch (RuntimeException | LinkageError error) {
            logger.log(Level.SEVERE, "Terminal reset event publication failed for " + outcome.operationId(), error);
        }
    }

    private String provider() {
        return gateway.providerName();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
