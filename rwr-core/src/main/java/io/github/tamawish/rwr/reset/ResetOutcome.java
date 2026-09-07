package io.github.tamawish.rwr.reset;

import java.util.Objects;

/**
 * Terminal or rejected outcome of a reset request.
 *
 * @param operationId unique operation identifier, or {@code none} before execution
 * @param worldId stable RWR world identifier
 * @param multiverseWorld provider-specific world name
 * @param phase final workflow phase
 * @param failure normalized failure reason, or {@code null} on success
 * @param safety retry-safety classification
 * @param message diagnostic outcome message
 */
public record ResetOutcome(
    String operationId,
    String worldId,
    String multiverseWorld,
    ResetPhase phase,
    ResetFailureType failure,
    FailureSafety safety,
    String message) {
  /** Validates the required identity, phase, safety, and message fields. */
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
    return new ResetOutcome(
        "none", worldId, multiverseWorld, ResetPhase.FAILED, failure, safety, message);
  }
}
