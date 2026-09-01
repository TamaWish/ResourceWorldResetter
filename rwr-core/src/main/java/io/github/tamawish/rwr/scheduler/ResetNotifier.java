package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.reset.ResetOutcome;

public interface ResetNotifier {
    ResetNotifier NONE = (world, outcome, broadcastCompletion) -> {};

    void terminal(ManagedWorldSettings world, ResetOutcome outcome, boolean broadcastCompletion);
}
