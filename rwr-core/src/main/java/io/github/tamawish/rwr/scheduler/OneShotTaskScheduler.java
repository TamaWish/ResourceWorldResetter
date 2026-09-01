package io.github.tamawish.rwr.scheduler;

import java.time.Duration;

public interface OneShotTaskScheduler {
    ScheduledTaskHandle schedule(Duration delay, Runnable task);
}
