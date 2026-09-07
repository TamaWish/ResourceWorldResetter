package io.github.tamawish.rwr.reset;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Executes the reset workflow for configured worlds. */
public interface ResetExecutor {
  ResetOutcome reset(String worldId);

  /**
   * Starts a reset without requiring the caller to block.
   *
   * @param worldId stable RWR world identifier
   * @return a stage completed with the reset outcome
   */
  default CompletionStage<ResetOutcome> resetAsync(String worldId) {
    try {
      return CompletableFuture.completedFuture(reset(worldId));
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
  }
}
