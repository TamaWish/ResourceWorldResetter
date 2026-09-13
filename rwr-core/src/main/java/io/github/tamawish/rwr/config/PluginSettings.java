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
 * @param defaultEvacuation defaults for newly configured worlds
 * @param proxyServers configured proxy server menu entries
 */
public record PluginSettings(
    int configVersion,
    ZoneId timezone,
    String defaultHubWorld,
    ResetPolicySettings resetPolicy,
    Map<String, ManagedWorldSettings> worlds,
    TeleportSettings teleport,
    EvacuationSettings defaultEvacuation,
    java.util.List<String> proxyServers) {
  public PluginSettings(
      int version,
      ZoneId zone,
      String hub,
      ResetPolicySettings policy,
      Map<String, ManagedWorldSettings> worlds,
      TeleportSettings teleport) {
    this(
        version,
        zone,
        hub,
        policy,
        worlds,
        teleport,
        new EvacuationSettings(true, hub),
        java.util.List.of());
  }

  public PluginSettings {
    proxyServers = java.util.List.copyOf(proxyServers);
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
