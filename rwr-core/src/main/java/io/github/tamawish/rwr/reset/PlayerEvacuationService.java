package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.EvacuationSettings;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Moves players out of a world before destructive reset work begins. */
public interface PlayerEvacuationService {
  EvacuationResult evacuate(String sourceWorld, EvacuationSettings settings);

  /**
   * Starts player evacuation without requiring the caller to block.
   *
   * @param sourceWorld provider-specific world name being evacuated
   * @param settings configured evacuation policy
   * @return a stage completed with the evacuation result
   */
  default CompletionStage<EvacuationResult> evacuateAsync(
      String sourceWorld, EvacuationSettings settings) {
    try {
      return CompletableFuture.completedFuture(evacuate(sourceWorld, settings));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }

  OptionalInt remainingPlayers(String sourceWorld);
}
