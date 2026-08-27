package com.lozaine.resourceworldresetter.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ResourceWorldPreResetEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String operationId;
    private final String worldId;
    private final String multiverseWorld;
    private boolean cancelled;

    public ResourceWorldPreResetEvent(String operationId, String worldId, String multiverseWorld) {
        this.operationId = operationId;
        this.worldId = worldId;
        this.multiverseWorld = multiverseWorld;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getWorldId() {
        return worldId;
    }

    public String getMultiverseWorld() {
        return multiverseWorld;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
