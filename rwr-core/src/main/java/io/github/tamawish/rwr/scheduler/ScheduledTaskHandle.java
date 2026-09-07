package io.github.tamawish.rwr.scheduler;

/** Cancellable handle to scheduled platform work. */
public interface ScheduledTaskHandle {
  void cancel();
}
