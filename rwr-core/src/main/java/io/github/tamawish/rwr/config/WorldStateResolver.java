package io.github.tamawish.rwr.config;

/** Derives operational state from configuration and the current provider catalog. */
public final class WorldStateResolver {
  private WorldStateResolver() {}

  /**
   * Resolves the current operational state of a configured world.
   *
   * @param multiverseWorld provider-specific world name or key
   * @param enabled whether resets are enabled in configuration
   * @param managed whether RWR is permitted to manage the world
   * @param hubWorld configured protected hub world
   * @param catalog current provider world catalog
   * @return derived operational state
   */
  public static WorldOperationalState resolve(
      String multiverseWorld,
      boolean enabled,
      boolean managed,
      String hubWorld,
      WorldCatalogView catalog) {
    if (!ConfigValidator.containsIgnoreCase(catalog.registeredWorldNames(), multiverseWorld)) {
      return WorldOperationalState.ORPHANED;
    }
    if (catalog.sameWorld(multiverseWorld, hubWorld)
        || catalog.sameWorld(multiverseWorld, catalog.defaultWorldName())) {
      return WorldOperationalState.PROTECTED;
    }
    return enabled && managed ? WorldOperationalState.MANAGED : WorldOperationalState.DISABLED;
  }
}
