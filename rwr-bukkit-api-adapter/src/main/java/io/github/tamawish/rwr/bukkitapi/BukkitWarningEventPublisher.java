package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.event.ResourceWorldResetWarningEvent;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.scheduler.WarningNotifier;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Server;

/** Publishes the public warning event and delegates the matching player notification. */
public final class BukkitWarningEventPublisher implements WarningNotifier {
    private final Server server;
    private final WarningNotifier playerWarnings;

    public BukkitWarningEventPublisher(Server server, WarningNotifier playerWarnings) {
        this.server = Objects.requireNonNull(server, "server");
        this.playerWarnings = Objects.requireNonNull(playerWarnings, "playerWarnings");
    }

    @Override
    public void warn(ManagedWorldSettings world, int minutesRemaining, ZonedDateTime resetAt) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(resetAt, "resetAt");
        try {
            server.getPluginManager().callEvent(new ResourceWorldResetWarningEvent(
                    world.id(), world.multiverseWorld(), minutesRemaining, resetAt.toInstant()));
        } catch (RuntimeException | LinkageError error) {
            server.getLogger().log(Level.SEVERE, "Scheduled warning event publication failed for " + world.id(), error);
        }
        playerWarnings.warn(world, minutesRemaining, resetAt);
    }
}
