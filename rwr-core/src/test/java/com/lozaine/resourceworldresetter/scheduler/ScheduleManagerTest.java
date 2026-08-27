package com.lozaine.resourceworldresetter.scheduler;

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
import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;
import com.lozaine.resourceworldresetter.reset.FailureSafety;
import com.lozaine.resourceworldresetter.reset.ResetExecutor;
import com.lozaine.resourceworldresetter.reset.ResetOutcome;
import com.lozaine.resourceworldresetter.reset.ResetPhase;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ScheduleManagerTest {
    @Test
    void warningsAreOneShotOrderedAndDeduplicated() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        List<Integer> warnings = new ArrayList<>();
        AtomicInteger resets = new AtomicInteger();
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                List.of(10, 1, 1)));
        ScheduleManager manager = manager(settings, resets, tasks, warnings, clock, ResetPhase.COMPLETE);

        manager.replaceSchedules(settings.get());
        tasks.runUntil(() -> resets.get() == 1);

        assertThat(warnings).containsExactly(10, 1);
        assertThat(resets).hasValue(1);
        assertThat(tasks.activeCount()).isEqualTo(3);
        assertThat(manager.nextRun("resource_id")).contains(
                ZonedDateTime.ofInstant(clock.instant().plus(Duration.ofMinutes(60)), ZoneId.of("UTC")));
    }

    @Test
    void reloadCancelsOldGenerationAndLeavesNoDuplicateTasks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        List<Integer> warnings = new ArrayList<>();
        AtomicInteger resets = new AtomicInteger();
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                List.of(10, 1)));
        ScheduleManager manager = manager(settings, resets, tasks, warnings, clock, ResetPhase.COMPLETE);

        manager.replaceSchedules(settings.get());
        List<FakeTask> firstGeneration = List.copyOf(tasks.tasks);
        manager.replaceSchedules(settings.get());

        assertThat(firstGeneration).allMatch(task -> task.cancelled);
        assertThat(tasks.activeCount()).isEqualTo(3);
        assertThat(manager.scheduledWorldCount()).isEqualTo(1);

        firstGeneration.forEach(task -> task.runnable.run());
        assertThat(warnings).isEmpty();
        assertThat(resets).hasValue(0);
    }

    @Test
    void reloadRecalculatesUsingNewTimezoneAndSchedule() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.DAILY, java.time.LocalTime.of(3, 0), null, 0, 0),
                List.of()));
        ScheduleManager manager = manager(settings, new AtomicInteger(), tasks, new ArrayList<>(), clock, ResetPhase.COMPLETE);
        manager.replaceSchedules(settings.get());

        PluginSettings replacement = settings(
                true,
                ZoneId.of("Asia/Kuala_Lumpur"),
                new ScheduleSettings(ScheduleType.DAILY, java.time.LocalTime.of(10, 0), null, 0, 0),
                List.of());
        settings.set(replacement);
        manager.replaceSchedules(replacement);

        assertThat(manager.nextRun("resource_id")).contains(
                ZonedDateTime.of(2026, 8, 27, 10, 0, 0, 0, ZoneId.of("Asia/Kuala_Lumpur")));
        assertThat(tasks.activeCount()).isEqualTo(1);
    }

    @Test
    void disabledWorldsHaveNoResetOrWarningTasks() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                List.of(10, 1)));
        ScheduleManager manager = manager(settings, new AtomicInteger(), tasks, new ArrayList<>(), clock, ResetPhase.COMPLETE);

        manager.replaceSchedules(settings.get());
        List<FakeTask> enabledTasks = List.copyOf(tasks.tasks);
        PluginSettings disabled = settings(
                false,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                List.of(10, 1));
        settings.set(disabled);
        manager.replaceSchedules(disabled);

        assertThat(enabledTasks).allMatch(task -> task.cancelled);
        assertThat(manager.scheduledWorldCount()).isZero();
        assertThat(tasks.activeCount()).isZero();
    }

    @Test
    void nonTerminalResetResultDoesNotCreateAnotherCycle() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        AtomicInteger resets = new AtomicInteger();
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 1),
                List.of()));
        ScheduleManager manager = manager(settings, resets, tasks, new ArrayList<>(), clock, ResetPhase.PRECHECK);
        manager.replaceSchedules(settings.get());

        tasks.runNext();

        assertThat(resets).hasValue(1);
        assertThat(tasks.activeCount()).isZero();
        assertThat(manager.nextRun("resource_id")).isEmpty();
    }

    @Test
    void manualResetCancelsPendingGenerationAndStartsNextOnlyAfterTerminalResult() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
        FakeTasks tasks = new FakeTasks(clock);
        AtomicInteger resets = new AtomicInteger();
        AtomicReference<PluginSettings> settings = new AtomicReference<>(settings(
                true,
                ZoneId.of("UTC"),
                new ScheduleSettings(ScheduleType.INTERVAL, null, null, 0, 60),
                List.of(10)));
        ScheduleManager manager = manager(settings, resets, tasks, new ArrayList<>(), clock, ResetPhase.COMPLETE);
        manager.replaceSchedules(settings.get());
        List<FakeTask> oldTasks = List.copyOf(tasks.tasks);

        ResetOutcome outcome = manager.resetNow("resource_id");

        assertThat(outcome.phase()).isEqualTo(ResetPhase.COMPLETE);
        assertThat(oldTasks).allMatch(task -> task.cancelled);
        assertThat(resets).hasValue(1);
        assertThat(tasks.activeCount()).isEqualTo(2);
    }

    private static ScheduleManager manager(
            AtomicReference<PluginSettings> settings,
            AtomicInteger resets,
            FakeTasks tasks,
            List<Integer> warnings,
            Clock clock,
            ResetPhase outcomePhase) {
        ResetExecutor executor = worldId -> {
            resets.incrementAndGet();
            return new ResetOutcome(
                    "operation",
                    worldId,
                    "resource",
                    outcomePhase,
                    null,
                    FailureSafety.NOT_RETRYABLE,
                    "test outcome");
        };
        return new ScheduleManager(
                settings::get,
                executor,
                new NextRunCalculator(),
                tasks,
                (world, minutes, resetAt) -> warnings.add(minutes),
                clock);
    }

    private static PluginSettings settings(
            boolean enabled,
            ZoneId zone,
            ScheduleSettings schedule,
            List<Integer> warnings) {
        WorldOperationalState state = enabled ? WorldOperationalState.MANAGED : WorldOperationalState.DISABLED;
        ManagedWorldSettings world = new ManagedWorldSettings(
                "resource_id",
                "resource",
                "Resource",
                enabled,
                true,
                schedule,
                warnings,
                new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                new EvacuationSettings(true, "world"),
                state);
        return new PluginSettings(
                5,
                zone,
                "world",
                new ResetPolicySettings(2, 30, true),
                Map.of(world.id(), world),
                new TeleportSettings(true, false, true, Map.of()));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void set(Instant value) {
            instant = value;
        }
    }

    private static final class FakeTasks implements OneShotTaskScheduler {
        private final MutableClock clock;
        private final List<FakeTask> tasks = new ArrayList<>();
        private long sequence;

        private FakeTasks(MutableClock clock) {
            this.clock = clock;
        }

        @Override
        public ScheduledTaskHandle schedule(Duration delay, Runnable task) {
            FakeTask scheduled = new FakeTask(clock.instant().plus(delay), sequence++, task);
            tasks.add(scheduled);
            return scheduled;
        }

        private int activeCount() {
            return (int) tasks.stream().filter(FakeTask::active).count();
        }

        private void runNext() {
            FakeTask next = tasks.stream()
                    .filter(FakeTask::active)
                    .min(Comparator.comparing((FakeTask task) -> task.at).thenComparingLong(task -> task.sequence))
                    .orElseThrow();
            clock.set(next.at);
            next.executed = true;
            next.runnable.run();
        }

        private void runUntil(java.util.function.BooleanSupplier condition) {
            int limit = 20;
            while (!condition.getAsBoolean() && limit-- > 0) {
                runNext();
            }
            if (!condition.getAsBoolean()) {
                throw new AssertionError("Condition was not reached before task limit");
            }
        }
    }

    private static final class FakeTask implements ScheduledTaskHandle {
        private final Instant at;
        private final long sequence;
        private final Runnable runnable;
        private boolean cancelled;
        private boolean executed;

        private FakeTask(Instant at, long sequence, Runnable runnable) {
            this.at = at;
            this.sequence = sequence;
            this.runnable = runnable;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        private boolean active() {
            return !cancelled && !executed;
        }
    }
}
