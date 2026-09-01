package io.github.tamawish.rwr.scheduler;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.plugin.Plugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Schedules one-shot delayed work on Folia's {@link org.bukkit.Bukkit#getGlobalRegionScheduler()}.
 * Callbacks run on the global region (not a world region).
 */
public final class FoliaOneShotTaskScheduler implements OneShotTaskScheduler {
    private static final long TICK_MILLIS = 50L;
    private final Plugin plugin;

    public FoliaOneShotTaskScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ScheduledTaskHandle schedule(Duration delay, Runnable task) {
        long millis = Math.max(0L, delay.toMillis());
        long ticks = Math.max(1L, Math.ceilDiv(millis, TICK_MILLIS));
        AtomicReference<ScheduledTask> handle = new AtomicReference<>();
        ScheduledTask scheduled = plugin.getServer().getGlobalRegionScheduler()
                .runDelayed(plugin, ignored -> task.run(), ticks);
        handle.set(scheduled);
        return () -> {
            ScheduledTask current = handle.get();
            if (current != null) {
                current.cancel();
            }
        };
    }
}
