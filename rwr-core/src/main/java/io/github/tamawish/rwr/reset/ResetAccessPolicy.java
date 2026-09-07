package io.github.tamawish.rwr.reset;

import java.util.Optional;

/** Exposes whether an active reset prevents incoming RWR teleports. */
public interface ResetAccessPolicy {
  boolean blocksIncomingRwrTeleport(String multiverseWorld);

  /**
   * Atomically admits an incoming RWR teleport when no reset owns the destination.
   *
   * <p>Implementations that do not track in-flight teleports retain the legacy check behavior.
   *
   * @param multiverseWorld provider-specific destination name
   * @return a permit that must be closed when teleportation finishes, or empty when blocked
   */
  default Optional<TeleportPermit> tryAcquireIncomingRwrTeleport(String multiverseWorld) {
    return blocksIncomingRwrTeleport(multiverseWorld)
        ? Optional.empty()
        : Optional.of(TeleportPermit.NONE);
  }

  /** Releases one admitted incoming teleport. */
  @FunctionalInterface
  interface TeleportPermit extends AutoCloseable {
    TeleportPermit NONE = () -> {};

    @Override
    void close();
  }
}
