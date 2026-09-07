package io.github.tamawish.rwr.config;

import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable root configuration consumed by core services.
 *
 * @param configVersion configuration schema version
 * @param timezone timezone used for calendar schedules
 * @param defaultHubWorld default safe hub world
 * @param resetPolicy global retry and completion policy
 * @param worlds managed worlds keyed by stable identifier
 * @param teleport player teleport-menu policy
 */
public record PluginSettings(
    int configVersion,
    ZoneId timezone,
    String defaultHubWorld,
    ResetPolicySettings resetPolicy,
    Map<String, ManagedWorldSettings> worlds,
    TeleportSettings teleport) {
  public PluginSettings {
    worlds = Collections.unmodifiableMap(new LinkedHashMap<>(worlds));
  }

  /**
   * Finds managed-world settings by stable identifier.
   *
   * @param id identifier to look up
   * @return matching settings, or an empty optional when absent
   */
  public Optional<ManagedWorldSettings> world(String id) {
    return Optional.ofNullable(worlds.get(id));
  }
}
