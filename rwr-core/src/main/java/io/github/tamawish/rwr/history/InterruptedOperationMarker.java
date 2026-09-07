package io.github.tamawish.rwr.history;

import io.github.tamawish.rwr.reset.ResetPhase;

/**
 * Durable marker used to reconcile a reset interrupted by process shutdown.
 *
 * @param operationId unique operation identifier
 * @param worldId stable RWR world identifier
 * @param multiverseWorld provider-specific world name
 * @param expectedWorldIdentity provider identity expected during verification
 * @param phase last durable workflow phase
 * @param startedAt operation start timestamp
 * @param updatedAt marker update timestamp
 */
public record InterruptedOperationMarker(
    String operationId,
    String worldId,
    String multiverseWorld,
    String expectedWorldIdentity,
    ResetPhase phase,
    String startedAt,
    String updatedAt) {
  /**
   * Copies this marker with a new durable phase.
   *
   * @param newPhase phase to persist
   * @param timestamp update timestamp
   * @return the updated marker
   */
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

  public InterruptedOperationMarker withExpectedWorldIdentity(
      String worldIdentity, String timestamp) {
    return new InterruptedOperationMarker(
        operationId, worldId, multiverseWorld, worldIdentity, phase, startedAt, timestamp);
  }
}
