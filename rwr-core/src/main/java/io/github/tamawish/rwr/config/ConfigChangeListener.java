package io.github.tamawish.rwr.config;

/** Receives an immutable configuration snapshot after a successful configuration change. */
@FunctionalInterface
public interface ConfigChangeListener {
  /**
   * Handles an accepted configuration change.
   *
   * @param settings newly active plugin settings
   */
  void onConfigChanged(PluginSettings settings);
}
