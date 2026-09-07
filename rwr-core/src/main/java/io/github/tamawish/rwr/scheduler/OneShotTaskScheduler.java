package io.github.tamawish.rwr.scheduler;

import java.time.Duration;

/** Schedules cancellable one-shot work without exposing platform scheduler types. */
public interface OneShotTaskScheduler {
  ScheduledTaskHandle schedule(Duration delay, Runnable task);
}
