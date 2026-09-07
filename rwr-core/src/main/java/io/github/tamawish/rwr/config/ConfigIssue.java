package io.github.tamawish.rwr.config;

/**
 * Describes one validation problem at a configuration path.
 *
 * @param path dotted path of the invalid setting
 * @param message explanation of the validation failure
 */
public record ConfigIssue(String path, String message) {
  @Override
  public String toString() {
    return path + ": " + message;
  }
}
