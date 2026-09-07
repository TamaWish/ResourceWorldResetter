package io.github.tamawish.rwr.config;

/** Describes whether a configured world can currently be managed and reset. */
public enum WorldOperationalState {
  MANAGED,
  DISABLED,
  PROTECTED,
  ORPHANED
}
