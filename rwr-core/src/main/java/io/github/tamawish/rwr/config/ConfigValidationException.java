package io.github.tamawish.rwr.config;

import java.util.List;

/** Reports all validation issues that prevented configuration persistence. */
public final class ConfigValidationException extends Exception {
  private final List<ConfigIssue> issues;

  /**
   * Creates an exception from the complete validation report.
   *
   * @param issues validation problems that rejected the configuration
   */
  public ConfigValidationException(List<ConfigIssue> issues) {
    super("Configuration is invalid: " + issues);
    this.issues = List.copyOf(issues);
  }

  public List<ConfigIssue> issues() {
    return issues;
  }
}
