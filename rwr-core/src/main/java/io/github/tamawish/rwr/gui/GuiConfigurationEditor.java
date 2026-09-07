package io.github.tamawish.rwr.gui;

import io.github.tamawish.rwr.config.ConfigService;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.config.ResetPolicySettings;
import io.github.tamawish.rwr.config.ScheduleSettings;
import io.github.tamawish.rwr.config.ScheduleType;
import io.github.tamawish.rwr.config.TeleportDestinationSettings;
import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.config.WorldOperationalState;
import io.github.tamawish.rwr.config.WorldStateResolver;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import io.github.tamawish.rwr.world.WorldProvider;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Transactional immutable edits shared by every admin screen. */
public final class GuiConfigurationEditor {
  private final ConfigService configs;
  private final WorldProvider gateway;

  public GuiConfigurationEditor(ConfigService configs, WorldProvider gateway) {
    this.configs = configs;
    this.gateway = gateway;
  }

  /**
   * Adds an RWR configuration for a provider world.
   *
   * @param multiverseWorld provider-specific world name
   * @return the accepted edit or a rejection explaining why it was unsafe
   */
  public GuiEditResult addWorld(String multiverseWorld) {
    PluginSettings current = configs.current();
    if (gateway.world(multiverseWorld).isEmpty()) {
      return GuiEditResult.rejected(
          "That world is not registered in " + gateway.providerName() + '.');
    }
    if (current.worlds().values().stream()
        .anyMatch(world -> world.multiverseWorld().equalsIgnoreCase(multiverseWorld))) {
      return GuiEditResult.rejected("That world already has an RWR configuration.");
    }
    if (multiverseWorld.equalsIgnoreCase(current.defaultHubWorld())
        || multiverseWorld.equalsIgnoreCase(gateway.defaultWorldName())) {
      return GuiEditResult.rejected("Protected hub/default worlds cannot be managed by RWR.");
    }

    String id = uniqueId(multiverseWorld, current.worlds());
    ManagedWorldSettings world =
        new ManagedWorldSettings(
            id,
            multiverseWorld,
            multiverseWorld,
            false,
            true,
            new ScheduleSettings(ScheduleType.DAILY, LocalTime.of(3, 0), null, 0, 0),
            List.of(30, 10, 5, 1),
            new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
            new EvacuationSettings(true, current.defaultHubWorld()),
            WorldOperationalState.DISABLED);
    return apply(
        settings -> {
          Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
          worlds.put(id, world);
          return copy(
              settings,
              worlds,
              settings.resetPolicy(),
              settings.teleport(),
              settings.timezone(),
              settings.defaultHubWorld());
        },
        "Added RWR configuration for " + multiverseWorld + ".");
  }

  /**
   * Removes an RWR configuration without deleting the provider world.
   *
   * @param id stable RWR world identifier
   * @return the edit outcome
   */
  public GuiEditResult removeWorld(String id) {
    if (!configs.current().worlds().containsKey(id)) {
      return GuiEditResult.rejected("That RWR configuration no longer exists.");
    }
    return apply(
        settings -> {
          Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
          worlds.remove(id);
          return copy(
              settings,
              worlds,
              settings.resetPolicy(),
              settings.teleport(),
              settings.timezone(),
              settings.defaultHubWorld());
        },
        "Removed the RWR configuration only. The "
            + gateway.providerName()
            + " world was not deleted.");
  }

  /**
   * Applies an immutable edit to one configured world.
   *
   * @param id stable RWR world identifier
   * @param edit transformation to apply to the current settings
   * @param message message returned after a successful edit
   * @return the edit outcome
   */
  public GuiEditResult updateWorld(
      String id, UnaryOperator<ManagedWorldSettings> edit, String message) {
    if (!configs.current().worlds().containsKey(id)) {
      return GuiEditResult.rejected("That RWR configuration no longer exists.");
    }
    return apply(
        settings -> {
          Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
          worlds.computeIfPresent(id, (ignored, world) -> edit.apply(world));
          return copy(
              settings,
              worlds,
              settings.resetPolicy(),
              settings.teleport(),
              settings.timezone(),
              settings.defaultHubWorld());
        },
        message);
  }

  /**
   * Changes the timezone used to calculate schedules.
   *
   * @param value new scheduling timezone
   * @return the edit outcome
   */
  public GuiEditResult setTimezone(ZoneId value) {
    return apply(
        settings ->
            copy(
                settings,
                settings.worlds(),
                settings.resetPolicy(),
                settings.teleport(),
                value,
                settings.defaultHubWorld()),
        "Timezone updated.");
  }

  /**
   * Changes the protected default hub world.
   *
   * @param value provider-specific hub world name
   * @return the edit outcome
   */
  public GuiEditResult setHub(String value) {
    if (gateway.world(value).isEmpty()) {
      return GuiEditResult.rejected(
          "The destination is not registered in " + gateway.providerName() + '.');
    }
    if (configs.current().worlds().values().stream()
        .anyMatch(world -> world.multiverseWorld().equalsIgnoreCase(value))) {
      return GuiEditResult.rejected(
          "Remove that world's RWR configuration before making it the protected hub.");
    }
    return apply(
        settings ->
            copy(
                settings,
                settings.worlds(),
                settings.resetPolicy(),
                settings.teleport(),
                settings.timezone(),
                value),
        "Default hub updated.");
  }

  /**
   * Applies an immutable edit to the global reset policy.
   *
   * @param edit transformation to apply to the current policy
   * @return the edit outcome
   */
  public GuiEditResult updateResetPolicy(UnaryOperator<ResetPolicySettings> edit) {
    return apply(
        settings ->
            copy(
                settings,
                settings.worlds(),
                edit.apply(settings.resetPolicy()),
                settings.teleport(),
                settings.timezone(),
                settings.defaultHubWorld()),
        "Reset policy updated.");
  }

  /**
   * Applies an immutable edit to teleport settings.
   *
   * @param edit transformation to apply to the current settings
   * @param message message returned after a successful edit
   * @return the edit outcome
   */
  public GuiEditResult updateTeleport(UnaryOperator<TeleportSettings> edit, String message) {
    return apply(
        settings ->
            copy(
                settings,
                settings.worlds(),
                settings.resetPolicy(),
                edit.apply(settings.teleport()),
                settings.timezone(),
                settings.defaultHubWorld()),
        message);
  }

  /**
   * Applies an immutable edit to one world's teleport override.
   *
   * @param worldName provider-specific world name
   * @param edit transformation to apply to the effective override
   * @param message message returned after a successful edit
   * @return the edit outcome
   */
  public GuiEditResult updateTeleportWorld(
      String worldName, UnaryOperator<TeleportDestinationSettings> edit, String message) {
    return updateTeleport(
        teleport -> {
          Map<String, TeleportDestinationSettings> worlds = new LinkedHashMap<>(teleport.worlds());
          TeleportDestinationSettings existing =
              worlds.getOrDefault(
                  worldName, new TeleportDestinationSettings(teleport.defaultEnabled(), null));
          worlds.put(worldName, edit.apply(existing));
          return new TeleportSettings(
              teleport.autoDiscover(), teleport.defaultEnabled(), teleport.showLocked(), worlds);
        },
        message);
  }

  /**
   * Removes a world-specific teleport override.
   *
   * @param worldName provider-specific world name
   * @return the edit outcome
   */
  public GuiEditResult removeTeleportOverride(String worldName) {
    return updateTeleport(
        teleport -> {
          Map<String, TeleportDestinationSettings> worlds = new LinkedHashMap<>(teleport.worlds());
          worlds.remove(worldName);
          return new TeleportSettings(
              teleport.autoDiscover(), teleport.defaultEnabled(), teleport.showLocked(), worlds);
        },
        "Teleport override removed; discovery defaults now apply.");
  }

  private GuiEditResult apply(UnaryOperator<PluginSettings> edit, String message) {
    PluginSettings candidate = normalize(edit.apply(configs.current()));
    ConfigService.ReloadResult saved = configs.saveAndApply(candidate);
    return saved.accepted()
        ? GuiEditResult.accepted(message)
        : new GuiEditResult(
            false, "Edit rejected; the previous configuration remains active.", saved.issues());
  }

  private PluginSettings normalize(PluginSettings settings) {
    Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>();
    settings
        .worlds()
        .forEach(
            (id, world) ->
                worlds.put(
                    id,
                    world.withState(
                        WorldStateResolver.resolve(
                            world.multiverseWorld(),
                            world.enabled(),
                            world.managed(),
                            settings.defaultHubWorld(),
                            gateway))));
    return copy(
        settings,
        worlds,
        settings.resetPolicy(),
        settings.teleport(),
        settings.timezone(),
        settings.defaultHubWorld());
  }

  private static PluginSettings copy(
      PluginSettings source,
      Map<String, ManagedWorldSettings> worlds,
      ResetPolicySettings policy,
      TeleportSettings teleport,
      ZoneId zone,
      String hub) {
    return new PluginSettings(source.configVersion(), zone, hub, policy, worlds, teleport);
  }

  /**
   * Copies world settings while selectively replacing editable values.
   *
   * @param source settings to copy
   * @param displayName replacement display name, or {@code null} to retain the current value
   * @param enabled replacement enabled state, or {@code null} to retain the current value
   * @param schedule replacement schedule, or {@code null} to retain the current value
   * @param warnings replacement warning minutes, or {@code null} to retain the current value
   * @param regeneration replacement regeneration settings, or {@code null} to retain them
   * @param evacuation replacement evacuation settings, or {@code null} to retain them
   * @return the copied settings
   */
  public static ManagedWorldSettings copyWorld(
      ManagedWorldSettings source,
      String displayName,
      Boolean enabled,
      ScheduleSettings schedule,
      List<Integer> warnings,
      RegenerationSettings regeneration,
      EvacuationSettings evacuation) {
    return new ManagedWorldSettings(
        source.id(),
        source.multiverseWorld(),
        displayName == null ? source.displayName() : displayName,
        enabled == null ? source.enabled() : enabled,
        source.managed(),
        schedule == null ? source.schedule() : schedule,
        warnings == null ? source.warnings() : warnings,
        regeneration == null ? source.regeneration() : regeneration,
        evacuation == null ? source.evacuation() : evacuation,
        source.state());
  }

  /**
   * Builds a schedule by replacing only the supplied values.
   *
   * @param world world whose current schedule supplies defaults
   * @param type replacement schedule type, or {@code null}
   * @param time replacement local time, or {@code null}
   * @param day replacement weekday, or {@code null}
   * @param monthDay replacement day of month, or {@code null}
   * @param interval replacement interval in minutes, or {@code null}
   * @return the updated schedule
   */
  public static ScheduleSettings schedule(
      ManagedWorldSettings world,
      ScheduleType type,
      LocalTime time,
      DayOfWeek day,
      Integer monthDay,
      Integer interval) {
    ScheduleSettings old = world.schedule();
    return new ScheduleSettings(
        type == null ? old.type() : type,
        time == null ? old.time() : time,
        day == null ? old.dayOfWeek() : day,
        monthDay == null ? old.dayOfMonth() : monthDay,
        interval == null ? old.intervalMinutes() : interval);
  }

  private static String uniqueId(String name, Map<String, ManagedWorldSettings> worlds) {
    String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_");
    if (base.isBlank()) {
      base = "world";
    }
    String candidate = base;
    int suffix = 2;
    while (worlds.containsKey(candidate)) {
      candidate = base + '_' + suffix++;
    }
    return candidate;
  }

  /**
   * Parses comma-separated positive warning minutes in descending order.
   *
   * @param input warning minutes, or {@code none} for an empty list
   * @return distinct warning minutes in descending order
   * @throws IllegalArgumentException if a value is not a positive integer
   */
  public static List<Integer> parseWarnings(String input) {
    if (input.isBlank() || input.equalsIgnoreCase("none")) {
      return List.of();
    }
    List<Integer> values = new ArrayList<>();
    for (String part : input.split(",")) {
      int value = Integer.parseInt(part.trim());
      if (value <= 0) {
        throw new IllegalArgumentException("Warning minutes must be positive whole numbers.");
      }
      if (!values.contains(value)) {
        values.add(value);
      }
    }
    values.sort((left, right) -> Integer.compare(right, left));
    return List.copyOf(values);
  }
}
