package com.lozaine.resourceworldresetter.reset;

import com.lozaine.resourceworldresetter.api.event.ResourceWorldPostResetEvent;
import com.lozaine.resourceworldresetter.api.event.ResourceWorldPreResetEvent;
import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import org.bukkit.Server;

public final class BukkitResetEventPublisher implements ResetEventPublisher {
    private final Server server;

    public BukkitResetEventPublisher(Server server) {
        this.server = server;
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
        server.getPluginManager().callEvent(new ResourceWorldPostResetEvent(
                outcome.operationId(),
                outcome.worldId(),
                outcome.multiverseWorld(),
                outcome.phase(),
                outcome.failure(),
                outcome.safety()));
    }
}
