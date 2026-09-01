package io.github.tamawish.rwr.config;

/** Resolves presentation-only names without changing stable IDs or Multiverse names. */
public final class WorldDisplayNames {
    private WorldDisplayNames() {}

    public static String resolve(PluginSettings settings, String multiverseWorld) {
        return settings.worlds().values().stream()
                .filter(world -> world.multiverseWorld().equalsIgnoreCase(multiverseWorld))
                .map(ManagedWorldSettings::displayName)
                .findFirst()
                .orElse(multiverseWorld);
    }

    public static String resolveId(PluginSettings settings, String worldId) {
        return settings.world(worldId)
                .map(ManagedWorldSettings::displayName)
                .orElse(worldId);
    }
}
