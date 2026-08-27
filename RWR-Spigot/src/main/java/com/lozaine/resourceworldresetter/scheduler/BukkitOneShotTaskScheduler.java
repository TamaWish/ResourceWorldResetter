package com.lozaine.resourceworldresetter.scheduler;

import java.time.Duration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitOneShotTaskScheduler implements OneShotTaskScheduler {
    private static final long TICK_MILLIS = 50L;
    private final Plugin plugin;

    public BukkitOneShotTaskScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledTaskHandle schedule(Duration delay, Runnable task) {
        long millis = Math.max(0L, delay.toMillis());
        long ticks = Math.max(1L, Math.ceilDiv(millis, TICK_MILLIS));
        BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, task, ticks);
        return bukkitTask::cancel;
    }
}
