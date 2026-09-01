package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.ManagedWorldSettings;

public interface ResetEventPublisher {
    ResetEventPublisher NONE = new ResetEventPublisher() {};

    default boolean beforeReset(ManagedWorldSettings world, String operationId) {
        return true;
    }

    default void afterReset(ResetOutcome outcome) {}
}
