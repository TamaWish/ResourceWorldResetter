package io.github.tamawish.rwr.history;

import io.github.tamawish.rwr.reset.FailureSafety;
import io.github.tamawish.rwr.reset.ResetFailureType;
import io.github.tamawish.rwr.reset.ResetPhase;

/**
 * Durable terminal history entry for a reset operation.
 *
 * @param operationId unique operation identifier
 * @param worldId stable RWR world identifier
 * @param multiverseWorld provider-specific world name
 * @param startedAt operation start timestamp
 * @param completedAt operation completion timestamp
 * @param terminalPhase final workflow phase
 * @param failure normalized failure reason, or {@code null} on success
 * @param safety retry-safety classification
 * @param message diagnostic outcome message
 */
public record ResetHistoryEntry(
    String operationId,
    String worldId,
    String multiverseWorld,
    String startedAt,
    String completedAt,
    ResetPhase terminalPhase,
    ResetFailureType failure,
    FailureSafety safety,
    String message) {}
