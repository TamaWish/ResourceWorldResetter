package io.github.tamawish.rwr.config;

import java.util.Set;

public interface WorldCatalogView {
    Set<String> registeredWorldNames();

    String defaultWorldName();

    /**
     * Paper/Worlds catalogs include namespaced keys ({@code minecraft:overworld}, {@code worlds:resource}).
     * Spigot/Multiverse catalogs use plain Bukkit world names only.
     */
    default boolean allowsNamespacedWorldNames() {
        return false;
    }
}
