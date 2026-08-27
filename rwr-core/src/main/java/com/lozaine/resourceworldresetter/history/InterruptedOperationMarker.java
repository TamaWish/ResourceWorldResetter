package com.lozaine.resourceworldresetter.history;

import com.lozaine.resourceworldresetter.reset.ResetPhase;

public record InterruptedOperationMarker(
        String operationId,
        String worldId,
        String multiverseWorld,
        String expectedWorldIdentity,
        ResetPhase phase,
        String startedAt,
        String updatedAt) {
    public InterruptedOperationMarker withPhase(ResetPhase newPhase, String timestamp) {
        return new InterruptedOperationMarker(
                operationId,
                worldId,
                multiverseWorld,
                expectedWorldIdentity,
                newPhase,
                startedAt,
                timestamp);
    }

    public InterruptedOperationMarker withExpectedWorldIdentity(String worldIdentity, String timestamp) {
        return new InterruptedOperationMarker(
                operationId,
                worldId,
                multiverseWorld,
                worldIdentity,
                phase,
                startedAt,
                timestamp);
    }
}
