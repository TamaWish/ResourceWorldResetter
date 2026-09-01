package io.github.tamawish.rwr.reset;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ResetExecutor {
    ResetOutcome reset(String worldId);

    default CompletionStage<ResetOutcome> resetAsync(String worldId) {
        try {
            return CompletableFuture.completedFuture(reset(worldId));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
