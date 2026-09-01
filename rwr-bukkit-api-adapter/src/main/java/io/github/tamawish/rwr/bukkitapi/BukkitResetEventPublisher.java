package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.event.ResourceWorldPreResetEvent;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.reset.ResetEventPublisher;
import io.github.tamawish.rwr.reset.ResetOutcome;
import java.util.Objects;
import org.bukkit.Server;

/** Shared Bukkit event publisher for both RWR platform variants. */
public final class BukkitResetEventPublisher implements ResetEventPublisher {
    private final Server server;

    public BukkitResetEventPublisher(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean beforeReset(ManagedWorldSettings world, String operationId) {
        ResourceWorldPreResetEvent event =
                new ResourceWorldPreResetEvent(operationId, world.id(), world.multiverseWorld());
        server.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    @Override
    public void afterReset(ResetOutcome outcome) {
        server.getPluginManager().callEvent(ApiMappings.postEvent(outcome));
    }
}
