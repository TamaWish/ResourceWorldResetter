package com.lozaine.resourceworldresetter.scheduler;

import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.config.PluginSettings;
import com.lozaine.resourceworldresetter.reset.FailureSafety;
import com.lozaine.resourceworldresetter.reset.ResetExecutor;
import com.lozaine.resourceworldresetter.reset.ResetOutcome;
import com.lozaine.resourceworldresetter.reset.ResetPhase;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class ScheduleManager implements AutoCloseable {
    private final Supplier<PluginSettings> settings;
    private final ResetExecutor resetExecutor;
    private final NextRunCalculator calculator;
    private final OneShotTaskScheduler tasks;
    private final WarningNotifier warnings;
    private final Clock clock;
    private final ResetNotifier resetNotifier;
    private final AtomicLong tokens = new AtomicLong();
    private final Map<String, WorldSchedule> schedules = new ConcurrentHashMap<>();

    public ScheduleManager(
            Supplier<PluginSettings> settings,
            ResetExecutor resetExecutor,
            NextRunCalculator calculator,
            OneShotTaskScheduler tasks,
            WarningNotifier warnings,
            Clock clock) {
        this(settings, resetExecutor, calculator, tasks, warnings, clock, ResetNotifier.NONE);
    }

    public ScheduleManager(
            Supplier<PluginSettings> settings,
            ResetExecutor resetExecutor,
            NextRunCalculator calculator,
            OneShotTaskScheduler tasks,
            WarningNotifier warnings,
            Clock clock,
            ResetNotifier resetNotifier) {
        this.settings = settings;
        this.resetExecutor = resetExecutor;
        this.calculator = calculator;
        this.tasks = tasks;
        this.warnings = warnings;
        this.clock = clock;
        this.resetNotifier = resetNotifier;
    }

    public synchronized void replaceSchedules(PluginSettings replacement) {
        cancelAll();
        ZonedDateTime now = now(replacement.timezone());
        for (ManagedWorldSettings world : replacement.worlds().values()) {
            if (world.canReset()) {
                scheduleNext(replacement, world, now);
            }
        }
    }

    public ResetOutcome resetNow(String worldId) {
        long token = beginManualReset(worldId);

        ResetOutcome outcome = resetExecutor.reset(worldId);
        completeReset(worldId, token, outcome);
        return outcome;
    }

    public CompletionStage<ResetOutcome> resetNowAsync(String worldId) {
        long token = beginManualReset(worldId);
        return resetExecutor.resetAsync(worldId).whenComplete((outcome, error) -> {
            if (error == null) {
                completeReset(worldId, token, outcome);
            }
        });
    }

    public synchronized Optional<ZonedDateTime> nextRun(String worldId) {
        WorldSchedule schedule = schedules.get(normalize(worldId));
        return schedule == null ? Optional.empty() : Optional.ofNullable(schedule.nextRun);
    }

    public synchronized int scheduledWorldCount() {
        return (int) schedules.values().stream().filter(schedule -> schedule.nextRun != null).count();
    }

    @Override
    public synchronized void close() {
        cancelAll();
    }

    private void scheduleNext(
            PluginSettings current,
            ManagedWorldSettings world,
            ZonedDateTime afterTerminal) {
        ZoneId zone = current.timezone();
        ZonedDateTime nextRun = calculator.nextRun(world.schedule(), zone, afterTerminal);
        long token = tokens.incrementAndGet();
        List<ScheduledTaskHandle> warningTasks = new ArrayList<>();

        Set<Integer> uniqueWarnings = new LinkedHashSet<>(world.warnings());
        uniqueWarnings.stream().sorted(Comparator.reverseOrder()).forEach(minutes -> {
            ZonedDateTime warningAt = nextRun.minusMinutes(minutes);
            if (warningAt.isAfter(afterTerminal)) {
                Duration delay = Duration.between(clock.instant(), warningAt.toInstant());
                warningTasks.add(tasks.schedule(
                        nonNegative(delay),
                        () -> fireWarning(world.id(), token, minutes, nextRun)));
            }
        });

        Duration resetDelay = Duration.between(clock.instant(), nextRun.toInstant());
        ScheduledTaskHandle resetTask = tasks.schedule(
                nonNegative(resetDelay),
                () -> fireReset(world.id(), token));
        schedules.put(
                normalize(world.id()),
                new WorldSchedule(token, nextRun, resetTask, warningTasks, true, 0));
    }

    private void scheduleSafeRetry(
            PluginSettings current,
            ManagedWorldSettings world,
            int retryAttempt) {
        long token = tokens.incrementAndGet();
        ZonedDateTime retryAt = now(current.timezone())
                .plusSeconds(current.resetPolicy().retryDelaySeconds());
        Duration delay = Duration.between(clock.instant(), retryAt.toInstant());
        ScheduledTaskHandle resetTask = tasks.schedule(
                nonNegative(delay),
                () -> fireReset(world.id(), token));
        schedules.put(
                normalize(world.id()),
                new WorldSchedule(token, retryAt, resetTask, List.of(), true, retryAttempt));
    }

    private void fireWarning(
            String worldId,
            long token,
            int minutes,
            ZonedDateTime resetAt) {
        synchronized (this) {
            WorldSchedule current = schedules.get(normalize(worldId));
            if (current == null || current.token != token || !resetAt.equals(current.nextRun)) {
                return;
            }
            ManagedWorldSettings world = settings.get().world(worldId).orElse(null);
            if (world == null || !world.canReset()) {
                return;
            }
            warnings.warn(world, minutes, resetAt);
        }
    }

    private void fireReset(String worldId, long token) {
        synchronized (this) {
            WorldSchedule current = schedules.get(normalize(worldId));
            if (current == null || current.token != token) {
                return;
            }
            current.cancelWarnings();
            current.nextRun = null;
            current.resetTask = null;
        }

        resetExecutor.resetAsync(worldId).whenComplete((outcome, error) -> {
            if (error == null) {
                completeReset(worldId, token, outcome);
            }
        });
    }

    private synchronized long beginManualReset(String worldId) {
        String key = normalize(worldId);
        WorldSchedule previous = schedules.remove(key);
        if (previous != null) {
            previous.cancel();
        }
        long token = tokens.incrementAndGet();
        schedules.put(key, WorldSchedule.running(token, false, 0));
        return token;
    }

    private void completeReset(String worldId, long token, ResetOutcome outcome) {
        notifyTerminal(worldId, outcome);
        rescheduleAfterTerminal(worldId, token, outcome);
    }

    private void notifyTerminal(String worldId, ResetOutcome outcome) {
        PluginSettings current = settings.get();
        current.world(worldId).ifPresent(world ->
                resetNotifier.terminal(world, outcome, current.resetPolicy().broadcastCompletion()));
    }

    private void rescheduleAfterTerminal(
            String worldId,
            long token,
            ResetOutcome outcome) {
        if (!isTerminal(outcome.phase())) {
            return;
        }
        synchronized (this) {
            WorldSchedule currentSchedule = schedules.get(normalize(worldId));
            if (currentSchedule == null || currentSchedule.token != token) {
                return;
            }
            schedules.remove(normalize(worldId));
            PluginSettings current = settings.get();
            ManagedWorldSettings currentWorld = current.world(worldId).orElse(null);
            if (currentWorld == null || !currentWorld.canReset()) {
                return;
            }
            if (outcome.safety() == FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
                    || outcome.phase() == ResetPhase.INTERRUPTED) {
                return;
            }
            if (currentSchedule.automatic
                    && outcome.safety() == FailureSafety.SAFE_TO_RETRY
                    && currentSchedule.safeRetryAttempts < current.resetPolicy().maxSafeRetries()) {
                scheduleSafeRetry(current, currentWorld, currentSchedule.safeRetryAttempts + 1);
            } else {
                scheduleNext(current, currentWorld, now(current.timezone()));
            }
        }
    }

    private void cancelAll() {
        schedules.values().forEach(WorldSchedule::cancel);
        schedules.clear();
    }

    private ZonedDateTime now(ZoneId zone) {
        return ZonedDateTime.ofInstant(clock.instant(), zone);
    }

    private static boolean isTerminal(ResetPhase phase) {
        return phase == ResetPhase.COMPLETE || phase == ResetPhase.FAILED || phase == ResetPhase.INTERRUPTED;
    }

    private static Duration nonNegative(Duration duration) {
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static final class WorldSchedule {
        private final long token;
        private final List<ScheduledTaskHandle> warningTasks;
        private final boolean automatic;
        private final int safeRetryAttempts;
        private ZonedDateTime nextRun;
        private ScheduledTaskHandle resetTask;

        private WorldSchedule(
                long token,
                ZonedDateTime nextRun,
                ScheduledTaskHandle resetTask,
                List<ScheduledTaskHandle> warningTasks,
                boolean automatic,
                int safeRetryAttempts) {
            this.token = token;
            this.nextRun = nextRun;
            this.resetTask = resetTask;
            this.warningTasks = new ArrayList<>(warningTasks);
            this.automatic = automatic;
            this.safeRetryAttempts = safeRetryAttempts;
        }

        private static WorldSchedule running(long token, boolean automatic, int safeRetryAttempts) {
            return new WorldSchedule(token, null, null, List.of(), automatic, safeRetryAttempts);
        }

        private void cancelWarnings() {
            warningTasks.forEach(ScheduledTaskHandle::cancel);
            warningTasks.clear();
        }

        private void cancel() {
            cancelWarnings();
            if (resetTask != null) {
                resetTask.cancel();
                resetTask = null;
            }
        }
    }
}
