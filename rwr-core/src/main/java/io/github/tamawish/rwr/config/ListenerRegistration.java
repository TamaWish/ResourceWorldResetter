package io.github.tamawish.rwr.config;

/** Represents a removable configuration-listener registration. */
@FunctionalInterface
public interface ListenerRegistration {
  /** Removes the associated listener; subsequent configuration changes are not delivered. */
  void unregister();
}
