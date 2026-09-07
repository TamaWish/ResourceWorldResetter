package io.github.tamawish.rwr.config;

/** Resolves presentation-only names without changing stable IDs or Multiverse names. */
public final class WorldDisplayNames {
  private WorldDisplayNames() {}

  /**
   * Resolves the configured display name for a provider world.
   *
   * @param settings active plugin settings
   * @param multiverseWorld provider-specific world name to resolve
   * @return the configured display name, or {@code multiverseWorld} when it is unmanaged
   */
  public static String resolve(PluginSettings settings, String multiverseWorld) {
    return settings.worlds().values().stream()
        .filter(world -> world.multiverseWorld().equalsIgnoreCase(multiverseWorld))
        .map(ManagedWorldSettings::displayName)
        .findFirst()
        .orElse(multiverseWorld);
  }

  public static String resolveId(PluginSettings settings, String worldId) {
    return settings.world(worldId).map(ManagedWorldSettings::displayName).orElse(worldId);
  }
}
