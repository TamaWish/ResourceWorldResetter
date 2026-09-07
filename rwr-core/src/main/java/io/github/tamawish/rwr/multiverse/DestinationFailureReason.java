package io.github.tamawish.rwr.multiverse;

/** Reasons that a safe destination could not be resolved. */
public enum DestinationFailureReason {
  NOT_REGISTERED,
  NOT_LOADED,
  INVALID_SPAWN_WORLD,
  NO_SAFE_LOCATION
}
