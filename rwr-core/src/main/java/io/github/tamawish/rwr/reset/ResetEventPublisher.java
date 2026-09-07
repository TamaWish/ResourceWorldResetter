package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.ManagedWorldSettings;

/** Publishes platform events around a reset operation. */
public interface ResetEventPublisher {
  ResetEventPublisher NONE = new ResetEventPublisher() {};

  default boolean beforeReset(ManagedWorldSettings world, String operationId) {
    return true;
  }

  default void afterReset(ResetOutcome outcome) {}
}
