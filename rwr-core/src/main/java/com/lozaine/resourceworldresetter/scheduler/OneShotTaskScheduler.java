package com.lozaine.resourceworldresetter.scheduler;

import java.time.Duration;

public interface OneShotTaskScheduler {
    ScheduledTaskHandle schedule(Duration delay, Runnable task);
}
