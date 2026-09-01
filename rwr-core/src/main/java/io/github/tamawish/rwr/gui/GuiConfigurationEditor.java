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

    public GuiEditResult addWorld(String multiverseWorld) {
        PluginSettings current = configs.current();
        if (gateway.world(multiverseWorld).isEmpty()) {
            return GuiEditResult.rejected("That world is not registered in " + gateway.providerName() + '.');
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
        ManagedWorldSettings world = new ManagedWorldSettings(
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
        return apply(settings -> {
            Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
            worlds.put(id, world);
            return copy(settings, worlds, settings.resetPolicy(), settings.teleport(), settings.timezone(),
                    settings.defaultHubWorld());
        }, "Added RWR configuration for " + multiverseWorld + ".");
    }

    public GuiEditResult removeWorld(String id) {
        if (!configs.current().worlds().containsKey(id)) {
            return GuiEditResult.rejected("That RWR configuration no longer exists.");
        }
        return apply(settings -> {
            Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
            worlds.remove(id);
            return copy(settings, worlds, settings.resetPolicy(), settings.teleport(), settings.timezone(),
                    settings.defaultHubWorld());
        }, "Removed the RWR configuration only. The " + gateway.providerName() + " world was not deleted.");
    }

    public GuiEditResult updateWorld(String id, UnaryOperator<ManagedWorldSettings> edit, String message) {
        if (!configs.current().worlds().containsKey(id)) {
            return GuiEditResult.rejected("That RWR configuration no longer exists.");
        }
        return apply(settings -> {
            Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>(settings.worlds());
            worlds.computeIfPresent(id, (ignored, world) -> edit.apply(world));
            return copy(settings, worlds, settings.resetPolicy(), settings.teleport(), settings.timezone(),
                    settings.defaultHubWorld());
        }, message);
    }

    public GuiEditResult setTimezone(ZoneId value) {
        return apply(settings -> copy(settings, settings.worlds(), settings.resetPolicy(), settings.teleport(), value,
                settings.defaultHubWorld()), "Timezone updated.");
    }

    public GuiEditResult setHub(String value) {
        if (gateway.world(value).isEmpty()) {
            return GuiEditResult.rejected("The destination is not registered in " + gateway.providerName() + '.');
        }
        if (configs.current().worlds().values().stream()
                .anyMatch(world -> world.multiverseWorld().equalsIgnoreCase(value))) {
            return GuiEditResult.rejected("Remove that world's RWR configuration before making it the protected hub.");
        }
        return apply(settings -> copy(settings, settings.worlds(), settings.resetPolicy(), settings.teleport(),
                settings.timezone(), value), "Default hub updated.");
    }

    public GuiEditResult updateResetPolicy(UnaryOperator<ResetPolicySettings> edit) {
        return apply(settings -> copy(settings, settings.worlds(), edit.apply(settings.resetPolicy()),
                settings.teleport(), settings.timezone(), settings.defaultHubWorld()), "Reset policy updated.");
    }

    public GuiEditResult updateTeleport(UnaryOperator<TeleportSettings> edit, String message) {
        return apply(settings -> copy(settings, settings.worlds(), settings.resetPolicy(), edit.apply(settings.teleport()),
                settings.timezone(), settings.defaultHubWorld()), message);
    }

    public GuiEditResult updateTeleportWorld(
            String worldName,
            UnaryOperator<TeleportDestinationSettings> edit,
            String message) {
        return updateTeleport(teleport -> {
            Map<String, TeleportDestinationSettings> worlds = new LinkedHashMap<>(teleport.worlds());
            TeleportDestinationSettings existing = worlds.getOrDefault(
                    worldName,
                    new TeleportDestinationSettings(teleport.defaultEnabled(), null));
            worlds.put(worldName, edit.apply(existing));
            return new TeleportSettings(teleport.autoDiscover(), teleport.defaultEnabled(), teleport.showLocked(), worlds);
        }, message);
    }

    public GuiEditResult removeTeleportOverride(String worldName) {
        return updateTeleport(teleport -> {
            Map<String, TeleportDestinationSettings> worlds = new LinkedHashMap<>(teleport.worlds());
            worlds.remove(worldName);
            return new TeleportSettings(teleport.autoDiscover(), teleport.defaultEnabled(), teleport.showLocked(), worlds);
        }, "Teleport override removed; discovery defaults now apply.");
    }

    private GuiEditResult apply(UnaryOperator<PluginSettings> edit, String message) {
        PluginSettings candidate = normalize(edit.apply(configs.current()));
        ConfigService.ReloadResult saved = configs.saveAndApply(candidate);
        return saved.accepted()
                ? GuiEditResult.accepted(message)
                : new GuiEditResult(false, "Edit rejected; the previous configuration remains active.", saved.issues());
    }

    private PluginSettings normalize(PluginSettings settings) {
        Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>();
        settings.worlds().forEach((id, world) -> worlds.put(id, world.withState(WorldStateResolver.resolve(
                world.multiverseWorld(), world.enabled(), world.managed(), settings.defaultHubWorld(), gateway))));
        return copy(settings, worlds, settings.resetPolicy(), settings.teleport(), settings.timezone(),
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

    public static ManagedWorldSettings copyWorld(
            ManagedWorldSettings source,
            String displayName,
            Boolean enabled,
            ScheduleSettings schedule,
            List<Integer> warnings,
            RegenerationSettings regeneration,
            EvacuationSettings evacuation) {
        return new ManagedWorldSettings(
                source.id(), source.multiverseWorld(), displayName == null ? source.displayName() : displayName,
                enabled == null ? source.enabled() : enabled, source.managed(),
                schedule == null ? source.schedule() : schedule,
                warnings == null ? source.warnings() : warnings,
                regeneration == null ? source.regeneration() : regeneration,
                evacuation == null ? source.evacuation() : evacuation,
                source.state());
    }

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
