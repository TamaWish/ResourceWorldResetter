package io.github.tamawish.rwr.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global and per-world settings for the player teleport menu.
 *
 * @param autoDiscover whether registered worlds are discovered automatically
 * @param defaultEnabled default availability for discovered destinations
 * @param showLocked whether destinations lacking permission remain visible
 * @param worlds per-world destination overrides
 */
public record TeleportSettings(
    boolean autoDiscover,
    boolean defaultEnabled,
    boolean showLocked,
    Map<String, TeleportDestinationSettings> worlds) {
  public TeleportSettings {
    worlds = Collections.unmodifiableMap(new LinkedHashMap<>(worlds));
  }
}
