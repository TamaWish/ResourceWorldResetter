package com.lozaine.resourceworldresetter.reset;

import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;

public interface ResetEventPublisher {
    ResetEventPublisher NONE = new ResetEventPublisher() {};

    default boolean beforeReset(ManagedWorldSettings world, String operationId) {
        return true;
    }

    default void afterReset(ResetOutcome outcome) {}
}
