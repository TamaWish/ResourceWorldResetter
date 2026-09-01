package io.github.tamawish.rwr.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Folia / Paper compatible scheduler helpers for entity, region, and async work.
 * Prefer GlobalRegionScheduler for non-region work.
 */
public final class FoliaScheduler {
    private final Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void runGlobal(Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
    }

    public ScheduledTask runGlobalDelayed(Runnable task, long delayTicks) {
        return Bukkit.getGlobalRegionScheduler()
                .runDelayed(plugin, ignored -> task.run(), Math.max(1, delayTicks));
    }

    public ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin, ignored -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
    }

    public void runLaterAsync(Runnable task, long delay, TimeUnit unit) {
        Bukkit.getAsyncScheduler().runDelayed(plugin, ignored -> task.run(), delay, unit);
    }

    public CompletableFuture<Void> delay(long millis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        long delay = Math.max(1L, millis);
        try {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                if (!future.isDone()) {
                    future.complete(null);
                }
            }, delay, TimeUnit.MILLISECONDS);
        } catch (Exception exception) {
            future.completeExceptionally(exception);
        }
        return future;
    }

    public void runAtLocation(Location location, Consumer<ScheduledTask> task) {
        Bukkit.getRegionScheduler().run(plugin, location, task);
    }

    public void runForEntity(Entity entity, Consumer<ScheduledTask> task) {
        entity.getScheduler().run(plugin, task, null);
    }

    public void runForWorld(World world, Runnable task) {
        runGlobal(task);
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
