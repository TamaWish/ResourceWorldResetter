package io.github.tamawish.rwr.reset;

public record ResetStatus(
        String worldId,
        String multiverseWorld,
        ResetPhase phase,
        String operationId,
        String message) {
    public static ResetStatus idle(String worldId, String multiverseWorld) {
        return new ResetStatus(worldId, multiverseWorld, ResetPhase.IDLE, "none", "Idle");
    }
}
