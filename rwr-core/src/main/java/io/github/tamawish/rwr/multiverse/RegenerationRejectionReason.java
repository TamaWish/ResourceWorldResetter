package io.github.tamawish.rwr.multiverse;

/** Precondition failures that reject provider world regeneration. */
public enum RegenerationRejectionReason {
  NOT_REGISTERED,
  NOT_LOADED,
  PROTECTED_DEFAULT_WORLD,
  PLAYERS_PRESENT
}
