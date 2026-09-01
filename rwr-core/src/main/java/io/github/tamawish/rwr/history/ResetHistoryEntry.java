package io.github.tamawish.rwr.history;

import io.github.tamawish.rwr.reset.FailureSafety;
import io.github.tamawish.rwr.reset.ResetFailureType;
import io.github.tamawish.rwr.reset.ResetPhase;

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
