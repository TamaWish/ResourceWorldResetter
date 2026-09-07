package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.history.ResetHistoryEntry;
import io.github.tamawish.rwr.reset.FailureSafety;
import io.github.tamawish.rwr.reset.ResetExecutor;
import io.github.tamawish.rwr.reset.ResetFailureType;
import io.github.tamawish.rwr.reset.ResetOutcome;
import io.github.tamawish.rwr.reset.ResetPhase;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** Owns reset and warning tasks for all currently schedulable worlds. */
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
  private final Set<String> pausedWorlds = ConcurrentHashMap.newKeySet();

  /**
   * Creates a schedule manager with terminal reset notifications.
   *
   * @param settings source of active plugin settings
   * @param resetExecutor reset workflow executor
   * @param calculator calculator for calendar-aware next runs
   * @param tasks platform one-shot scheduler
   * @param warnings advance-warning notifier
   * @param clock clock used to calculate task delays
   */
  public ScheduleManager(
      Supplier<PluginSettings> settings,
      ResetExecutor resetExecutor,
      NextRunCalculator calculator,
      OneShotTaskScheduler tasks,
      WarningNotifier warnings,
      Clock clock) {
    this(settings, resetExecutor, calculator, tasks, warnings, clock, ResetNotifier.NONE);
  }

  /**
   * Creates a schedule manager with terminal reset notifications.
   *
   * @param settings source of active plugin settings
   * @param resetExecutor reset workflow executor
   * @param calculator calculator for calendar-aware next runs
   * @param tasks platform one-shot scheduler
   * @param warnings advance-warning notifier
   * @param clock clock used to calculate task delays
   * @param resetNotifier terminal reset notifier
   */
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

  /**
   * Reconciles pending tasks with replacement settings while retaining active resets.
   *
   * @param replacement newly activated plugin settings
   */
  public synchronized void replaceSchedules(PluginSettings replacement) {
    schedules
        .entrySet()
        .removeIf(
            entry -> {
              if (entry.getValue().running) {
                return false;
              }
              entry.getValue().cancel();
              return true;
            });
    ZonedDateTime now = now(replacement.timezone());
    for (ManagedWorldSettings world : replacement.worlds().values()) {
      if (world.canReset()
          && !schedules.containsKey(normalize(world.id()))
          && !pausedWorlds.contains(normalize(world.id()))) {
        scheduleNext(replacement, world, now);
      }
    }
  }

  /**
   * Runs an immediate reset unless the world already has an active reset.
   *
   * @param worldId stable RWR world identifier
   * @return terminal or rejected reset outcome
   */
  public ResetOutcome resetNow(String worldId) {
    long token = beginManualReset(worldId);
    if (token < 0) {
      return busyOutcome(worldId);
    }
    ResetOutcome outcome;
    try {
      outcome = terminalOutcome(worldId, resetExecutor.reset(worldId), null);
    } catch (RuntimeException exception) {
      outcome = terminalOutcome(worldId, null, exception);
    }
    completeReset(worldId, token, outcome);
    return outcome;
  }

  /**
   * Starts an immediate reset without blocking the caller.
   *
   * @param worldId stable RWR world identifier
   * @return a stage completed with the terminal or rejected outcome
   */
  public CompletionStage<ResetOutcome> resetNowAsync(String worldId) {
    return resetNowAsync(worldId, true);
  }

  /**
   * Starts an immediate reset with explicit terminal-notification routing.
   *
   * @param worldId stable RWR world identifier
   * @param notifyTerminal whether the configured global notifier should receive the outcome
   * @return a stage completed with the terminal or rejected outcome
   */
  public CompletionStage<ResetOutcome> resetNowAsync(String worldId, boolean notifyTerminal) {
    long token = beginManualReset(worldId);
    if (token < 0) {
      return CompletableFuture.completedFuture(busyOutcome(worldId));
    }
    return executeAsync(worldId)
        .handle((outcome, error) -> terminalOutcome(worldId, outcome, error))
        .whenComplete((outcome, error) -> completeReset(worldId, token, outcome, notifyTerminal));
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
      PluginSettings current, ManagedWorldSettings world, ZonedDateTime afterTerminal) {
    ZoneId zone = current.timezone();
    ZonedDateTime nextRun = calculator.nextRun(world.schedule(), zone, afterTerminal);
    long token = tokens.incrementAndGet();
    List<ScheduledTaskHandle> warningTasks = new ArrayList<>();

    Set<Integer> uniqueWarnings = new LinkedHashSet<>(world.warnings());
    uniqueWarnings.stream()
        .sorted(Comparator.reverseOrder())
        .forEach(
            minutes -> {
              ZonedDateTime warningAt = nextRun.minusMinutes(minutes);
              if (warningAt.isAfter(afterTerminal)) {
                Duration delay = Duration.between(clock.instant(), warningAt.toInstant());
                warningTasks.add(
                    tasks.schedule(
                        nonNegative(delay),
                        () -> fireWarning(world.id(), token, minutes, nextRun)));
              }
            });

    Duration resetDelay = Duration.between(clock.instant(), nextRun.toInstant());
    ScheduledTaskHandle resetTask =
        tasks.schedule(nonNegative(resetDelay), () -> fireReset(world.id(), token));
    schedules.put(
        normalize(world.id()), new WorldSchedule(token, nextRun, resetTask, warningTasks, true, 0));
  }

  private void scheduleSafeRetry(
      PluginSettings current, ManagedWorldSettings world, int retryAttempt) {
    long token = tokens.incrementAndGet();
    ZonedDateTime retryAt =
        now(current.timezone()).plusSeconds(current.resetPolicy().retryDelaySeconds());
    Duration delay = Duration.between(clock.instant(), retryAt.toInstant());
    ScheduledTaskHandle resetTask =
        tasks.schedule(nonNegative(delay), () -> fireReset(world.id(), token));
    schedules.put(
        normalize(world.id()),
        new WorldSchedule(token, retryAt, resetTask, List.of(), true, retryAttempt));
  }

  private void fireWarning(String worldId, long token, int minutes, ZonedDateTime resetAt) {
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
      if (current == null || current.token != token || current.running) {
        return;
      }
      current.cancelWarnings();
      current.nextRun = null;
      current.resetTask = null;
      current.running = true;
    }

    executeAsync(worldId)
        .handle((outcome, error) -> terminalOutcome(worldId, outcome, error))
        .whenComplete((outcome, error) -> completeReset(worldId, token, outcome, true));
  }

  private synchronized long beginManualReset(String worldId) {
    String key = normalize(worldId);
    WorldSchedule active = schedules.get(key);
    if (active != null && active.running) {
      return -1;
    }
    pausedWorlds.remove(key);
    WorldSchedule previous = schedules.remove(key);
    if (previous != null) {
      previous.cancel();
    }
    long token = tokens.incrementAndGet();
    schedules.put(key, WorldSchedule.running(token, false, 0));
    return token;
  }

  /**
   * Restores scheduling holds for interrupted or ambiguous reset history.
   *
   * @param history recovered terminal history entries
   */
  public synchronized void restoreSafetyHolds(List<ResetHistoryEntry> history) {
    for (ResetHistoryEntry entry : history) {
      String key = normalize(entry.worldId());
      if (entry.safety() == FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
          || entry.terminalPhase() == ResetPhase.INTERRUPTED) {
        pausedWorlds.add(key);
        WorldSchedule schedule = schedules.remove(key);
        if (schedule != null) {
          schedule.cancel();
        }
      } else {
        pausedWorlds.remove(key);
      }
    }
  }

  private CompletionStage<ResetOutcome> executeAsync(String worldId) {
    try {
      return resetExecutor.resetAsync(worldId);
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  private ResetOutcome busyOutcome(String worldId) {
    String worldName =
        settings.get().world(worldId).map(ManagedWorldSettings::multiverseWorld).orElse("unknown");
    return ResetOutcome.rejected(
        worldId,
        worldName,
        ResetFailureType.WORLD_BUSY,
        FailureSafety.SAFE_TO_RETRY,
        "A reset is already active for this world.");
  }

  private void completeReset(String worldId, long token, ResetOutcome outcome) {
    completeReset(worldId, token, outcome, true);
  }

  private void completeReset(
      String worldId, long token, ResetOutcome outcome, boolean notifyTerminal) {
    try {
      if (notifyTerminal) {
        notifyTerminal(worldId, outcome);
      }
    } finally {
      rescheduleAfterTerminal(worldId, token, outcome);
    }
  }

  private ResetOutcome terminalOutcome(String worldId, ResetOutcome outcome, Throwable error) {
    if (error == null && outcome != null) {
      return outcome;
    }
    ManagedWorldSettings world = settings.get().world(worldId).orElse(null);
    String worldName = world == null ? "unknown" : world.multiverseWorld();
    String detail =
        error == null
            ? "Reset executor returned no outcome."
            : error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
    return ResetOutcome.rejected(
        worldId,
        worldName,
        ResetFailureType.MULTIVERSE_API_EXCEPTION,
        FailureSafety.AMBIGUOUS_REVIEW_REQUIRED,
        "Reset executor failed exceptionally: " + detail);
  }

  private void notifyTerminal(String worldId, ResetOutcome outcome) {
    PluginSettings current = settings.get();
    current
        .world(worldId)
        .ifPresent(
            world ->
                resetNotifier.terminal(
                    world, outcome, current.resetPolicy().broadcastCompletion()));
  }

  private void rescheduleAfterTerminal(String worldId, long token, ResetOutcome outcome) {
    if (!isTerminal(outcome.phase())) {
      return;
    }
    synchronized (this) {
      WorldSchedule currentSchedule = schedules.get(normalize(worldId));
      if (currentSchedule == null || currentSchedule.token != token) {
        return;
      }
      schedules.remove(normalize(worldId));
      if (outcome.safety() == FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
          || outcome.phase() == ResetPhase.INTERRUPTED) {
        pausedWorlds.add(normalize(worldId));
        return;
      }
      PluginSettings current = settings.get();
      ManagedWorldSettings currentWorld = current.world(worldId).orElse(null);
      if (currentWorld == null || !currentWorld.canReset()) {
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
    return phase == ResetPhase.COMPLETE
        || phase == ResetPhase.FAILED
        || phase == ResetPhase.INTERRUPTED;
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
    private boolean running;

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
      WorldSchedule schedule =
          new WorldSchedule(token, null, null, List.of(), automatic, safeRetryAttempts);
      schedule.running = true;
      return schedule;
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
