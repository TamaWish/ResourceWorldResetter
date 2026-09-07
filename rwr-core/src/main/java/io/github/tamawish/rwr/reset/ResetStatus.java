package io.github.tamawish.rwr.reset;

/**
 * Current observable state of a configured world's reset workflow.
 *
 * @param worldId stable RWR world identifier
 * @param multiverseWorld provider-specific world name
 * @param phase current workflow phase
 * @param operationId active operation identifier, or {@code none}
 * @param message diagnostic status message
 */
public record ResetStatus(
    String worldId, String multiverseWorld, ResetPhase phase, String operationId, String message) {
  public static ResetStatus idle(String worldId, String multiverseWorld) {
    return new ResetStatus(worldId, multiverseWorld, ResetPhase.IDLE, "none", "Idle");
  }
}
