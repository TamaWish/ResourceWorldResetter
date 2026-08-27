package com.lozaine.resourceworldresetter.scheduler;

import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.reset.ResetOutcome;

public interface ResetNotifier {
    ResetNotifier NONE = (world, outcome, broadcastCompletion) -> {};

    void terminal(ManagedWorldSettings world, ResetOutcome outcome, boolean broadcastCompletion);
}
