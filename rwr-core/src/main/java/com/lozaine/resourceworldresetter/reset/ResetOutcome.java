package com.lozaine.resourceworldresetter.reset;

import java.util.Objects;

public record ResetOutcome(
        String operationId,
        String worldId,
        String multiverseWorld,
        ResetPhase phase,
        ResetFailureType failure,
        FailureSafety safety,
        String message) {
    public ResetOutcome {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(multiverseWorld, "multiverseWorld");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(safety, "safety");
        Objects.requireNonNull(message, "message");
    }

    public boolean successful() {
        return phase == ResetPhase.COMPLETE;
    }

    public static ResetOutcome rejected(
            String worldId,
            String multiverseWorld,
            ResetFailureType failure,
            FailureSafety safety,
            String message) {
        return new ResetOutcome("none", worldId, multiverseWorld, ResetPhase.FAILED, failure, safety, message);
    }
}
