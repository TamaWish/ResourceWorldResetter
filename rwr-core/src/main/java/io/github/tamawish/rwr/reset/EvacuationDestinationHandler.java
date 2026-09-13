package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.EvacuationDestination;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** A transfer request; completion never substitutes for checking source-world occupancy. */
@FunctionalInterface
public interface EvacuationDestinationHandler {
  CompletionStage<Boolean> evacuate(UUID playerId, EvacuationDestination destination);
}
