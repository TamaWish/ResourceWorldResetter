package io.github.tamawish.rwr.multiverse;

/** Upstream failures that can prevent provider world regeneration. */
public enum RegenerationFailureReason {
  DELETE_FAILED,
  CREATE_FAILED,
  API_EXCEPTION
}
