package io.github.tamawish.rwr.world;

import io.github.tamawish.rwr.config.WorldCatalogView;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.multiverse.RegenerationOutcome;
import io.github.tamawish.rwr.multiverse.RegenerationRequest;
import io.github.tamawish.rwr.multiverse.WorldSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Platform port for world catalog, safe destinations, and regeneration.
 * Implemented by Multiverse (Spigot) and Worlds (Paper-Folia).
 */
public interface WorldProvider extends WorldCatalogView {
    /** Human-readable authority name used in status, history, and GUI messages. */
    default String providerName() {
        return "world provider";
    }

    List<WorldSnapshot> registeredWorlds();

    List<WorldSnapshot> loadedWorlds();

    default List<WorldSnapshot> worlds() {
        return registeredWorlds();
    }

    Optional<WorldSnapshot> world(String name);

    DestinationResult resolveSafeDestination(String name);

    RegenerationOutcome regenerate(RegenerationRequest request);

    /**
     * Starts regeneration without requiring the caller to block for a platform-owned future.
     * Synchronous providers inherit the completed-future adapter.
     */
    default CompletionStage<RegenerationOutcome> regenerateAsync(RegenerationRequest request) {
        try {
            return CompletableFuture.completedFuture(regenerate(request));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
