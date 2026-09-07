package io.github.tamawish.rwr.config;

import java.util.Set;

/** Supplies the world names and identity rules used while validating configuration. */
public interface WorldCatalogView {
  Set<String> registeredWorldNames();

  String defaultWorldName();

  default String canonicalWorldName(String name) {
    return name;
  }

  default boolean sameWorld(String first, String second) {
    return canonicalWorldName(first).equalsIgnoreCase(canonicalWorldName(second));
  }

  /**
   * Paper/Worlds catalogs include namespaced keys ({@code minecraft:overworld}, {@code
   * worlds:resource}). Spigot/Multiverse catalogs use plain Bukkit world names only.
   */
  default boolean allowsNamespacedWorldNames() {
    return false;
  }
}
