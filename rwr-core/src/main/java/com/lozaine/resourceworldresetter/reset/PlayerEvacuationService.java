package com.lozaine.resourceworldresetter.reset;

import com.lozaine.resourceworldresetter.config.EvacuationSettings;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface PlayerEvacuationService {
    EvacuationResult evacuate(String sourceWorld, EvacuationSettings settings);

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
