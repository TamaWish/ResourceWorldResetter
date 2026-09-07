package io.github.tamawish.rwr.config;

import java.util.List;

/**
 * Result of loading and validating the plugin configuration.
 *
 * @param status overall load status
 * @param settings parsed settings when the result is valid, otherwise {@code null}
 * @param issues validation or migration issues discovered during loading
 */
public record ConfigLoadResult(
    ConfigLoadStatus status, PluginSettings settings, List<ConfigIssue> issues) {
  public ConfigLoadResult {
    issues = List.copyOf(issues);
  }

  public boolean valid() {
    return status == ConfigLoadStatus.VALID && settings != null;
  }

  /**
   * Creates a successful load result.
   *
   * @param settings validated settings to expose
   * @return a valid result without issues
   */
  public static ConfigLoadResult valid(PluginSettings settings) {
    return new ConfigLoadResult(ConfigLoadStatus.VALID, settings, List.of());
  }

  /**
   * Creates a result for an invalid configuration.
   *
   * @param issues validation problems that prevent activation
   * @return an invalid result without active settings
   */
  public static ConfigLoadResult invalid(List<ConfigIssue> issues) {
    return new ConfigLoadResult(ConfigLoadStatus.INVALID, null, issues);
  }

  /**
   * Creates a result for a configuration requiring migration.
   *
   * @param issues migration requirements discovered during loading
   * @return a migration-required result without active settings
   */
  public static ConfigLoadResult migrationRequired(List<ConfigIssue> issues) {
    return new ConfigLoadResult(ConfigLoadStatus.MIGRATION_REQUIRED, null, issues);
  }
}
