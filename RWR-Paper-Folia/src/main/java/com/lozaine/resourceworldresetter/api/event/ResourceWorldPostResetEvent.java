package com.lozaine.resourceworldresetter.api.event;

import com.lozaine.resourceworldresetter.reset.FailureSafety;
import com.lozaine.resourceworldresetter.reset.ResetFailureType;
import com.lozaine.resourceworldresetter.reset.ResetPhase;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ResourceWorldPostResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String operationId;
    private final String worldId;
    private final String multiverseWorld;
    private final ResetPhase phase;
    private final ResetFailureType failure;
    private final FailureSafety safety;

    public ResourceWorldPostResetEvent(
            String operationId,
            String worldId,
            String multiverseWorld,
            ResetPhase phase,
            @Nullable ResetFailureType failure,
            FailureSafety safety) {
        this.operationId = operationId;
        this.worldId = worldId;
        this.multiverseWorld = multiverseWorld;
        this.phase = phase;
        this.failure = failure;
        this.safety = safety;
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

    public ResetPhase getPhase() {
        return phase;
    }

    public @Nullable ResetFailureType getFailure() {
        return failure;
    }

    public FailureSafety getSafety() {
        return safety;
    }

    public boolean isSuccessful() {
        return phase == ResetPhase.COMPLETE;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
