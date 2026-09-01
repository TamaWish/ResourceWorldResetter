package io.github.tamawish.rwr.config;

public final class WorldStateResolver {
    private WorldStateResolver() {}

    public static WorldOperationalState resolve(
            String multiverseWorld,
            boolean enabled,
            boolean managed,
            String hubWorld,
            WorldCatalogView catalog) {
        if (!ConfigValidator.containsIgnoreCase(catalog.registeredWorldNames(), multiverseWorld)) {
            return WorldOperationalState.ORPHANED;
        }
        if (multiverseWorld.equalsIgnoreCase(hubWorld)
                || multiverseWorld.equalsIgnoreCase(catalog.defaultWorldName())) {
            return WorldOperationalState.PROTECTED;
        }
        return enabled && managed ? WorldOperationalState.MANAGED : WorldOperationalState.DISABLED;
    }
}
