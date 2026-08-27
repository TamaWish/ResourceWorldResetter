package com.lozaine.resourceworldresetter.history;

import com.lozaine.resourceworldresetter.reset.FailureSafety;
import com.lozaine.resourceworldresetter.reset.ResetFailureType;
import com.lozaine.resourceworldresetter.reset.ResetPhase;

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
