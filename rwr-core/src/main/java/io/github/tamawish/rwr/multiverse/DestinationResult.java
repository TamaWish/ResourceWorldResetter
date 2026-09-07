package io.github.tamawish.rwr.multiverse;

import io.github.tamawish.rwr.world.SafeLocation;

/** Result of resolving a safe destination through a world provider. */
public sealed interface DestinationResult
    permits DestinationResult.Available, DestinationResult.Unavailable {
  /**
   * A resolved safe destination.
   *
   * @param location resolved location
   * @param adjusted whether safety checks moved the requested location
   */
  record Available(SafeLocation location, boolean adjusted) implements DestinationResult {}

  /**
   * A destination that could not be resolved.
   *
   * @param reason machine-readable failure reason
   * @param message diagnostic explanation
   */
  record Unavailable(DestinationFailureReason reason, String message)
      implements DestinationResult {}
}
