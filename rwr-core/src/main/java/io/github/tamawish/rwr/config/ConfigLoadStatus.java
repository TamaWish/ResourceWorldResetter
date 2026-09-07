package io.github.tamawish.rwr.config;

/** Classifies whether loaded configuration data can be activated. */
public enum ConfigLoadStatus {
  VALID,
  INVALID,
  MIGRATION_REQUIRED
}
