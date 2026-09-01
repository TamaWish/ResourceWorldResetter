package io.github.tamawish.rwr.config;

import io.github.tamawish.rwr.multiverse.SeedPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ConfigValidator {
    private static final Pattern WORLD_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

    public List<ConfigIssue> validate(PluginSettings settings, WorldCatalogView catalog) {
        List<ConfigIssue> issues = new ArrayList<>();
        if (settings.configVersion() != 5) {
            issues.add(new ConfigIssue("config-version", "must be exactly 5"));
        }
        if (settings.defaultHubWorld().isBlank()) {
            issues.add(new ConfigIssue("default-hub-world", "must not be blank"));
        } else if (isNamespacedWorldName(settings.defaultHubWorld()) && !catalog.allowsNamespacedWorldNames()) {
            issues.add(new ConfigIssue("default-hub-world", "must be a plain Bukkit world name without ':'"));
        } else if (!containsIgnoreCase(catalog.registeredWorldNames(), settings.defaultHubWorld())) {
            issues.add(new ConfigIssue("default-hub-world", notRegisteredMessage(catalog)));
        }
        if (settings.resetPolicy().maxSafeRetries() < 0) {
            issues.add(new ConfigIssue("reset-policy.max-safe-retries", "must be zero or greater"));
        }
        if (settings.resetPolicy().retryDelaySeconds() < 0) {
            issues.add(new ConfigIssue("reset-policy.retry-delay-seconds", "must be zero or greater"));
        }

        Set<String> multiverseNames = new HashSet<>();
        for (var entry : settings.worlds().entrySet()) {
            ManagedWorldSettings world = entry.getValue();
            String path = "worlds." + world.id();
            if (!entry.getKey().equals(world.id())) {
                issues.add(new ConfigIssue("worlds." + entry.getKey(), "map key must match the stable world ID"));
            }
            if (!WORLD_ID.matcher(world.id()).matches()) {
                issues.add(new ConfigIssue(path, "ID must match " + WORLD_ID.pattern()));
            }
            if (!multiverseNames.add(world.multiverseWorld().toLowerCase(Locale.ROOT))) {
                issues.add(new ConfigIssue(path + ".multiverse-world", "is configured under more than one ID"));
            }
            if (world.multiverseWorld().isBlank()) {
                issues.add(new ConfigIssue(path + ".multiverse-world", "must not be blank"));
            } else if (isNamespacedWorldName(world.multiverseWorld()) && !catalog.allowsNamespacedWorldNames()) {
                issues.add(new ConfigIssue(
                        path + ".multiverse-world",
                        "must be a plain Multiverse world name such as 'resource', not a namespaced key"));
            }
            if (world.displayName().isBlank()) {
                issues.add(new ConfigIssue(path + ".display-name", "must not be blank"));
            }
            if (world.managed() && isProtected(world.multiverseWorld(), settings, catalog)) {
                issues.add(new ConfigIssue(path + ".managed", "the default or hub world cannot be managed"));
            }
            WorldOperationalState expected = expectedState(world, settings, catalog);
            if (world.state() != expected) {
                issues.add(new ConfigIssue(path, "operational state must be " + expected));
            }
            validateSchedule(path, world.schedule(), issues);
            validateWarnings(path, world.warnings(), issues);
            validateRegeneration(path, world.regeneration(), issues);
            validateEvacuation(path, world, catalog, issues);
        }
        validateTeleport(settings.teleport(), catalog, issues);
        return List.copyOf(issues);
    }

    private static WorldOperationalState expectedState(
            ManagedWorldSettings world, PluginSettings settings, WorldCatalogView catalog) {
        return WorldStateResolver.resolve(
                world.multiverseWorld(),
                world.enabled(),
                world.managed(),
                settings.defaultHubWorld(),
                catalog);
    }

    private static void validateSchedule(String path, ScheduleSettings schedule, List<ConfigIssue> issues) {
        if (schedule == null || schedule.type() == null) {
            issues.add(new ConfigIssue(path + ".schedule", "type is required"));
            return;
        }
        switch (schedule.type()) {
            case DAILY -> requireTime(path, schedule, issues);
            case WEEKLY -> {
                requireTime(path, schedule, issues);
                if (schedule.dayOfWeek() == null) {
                    issues.add(new ConfigIssue(path + ".schedule.day-of-week", "is required for WEEKLY"));
                }
            }
            case MONTHLY -> {
                requireTime(path, schedule, issues);
                if (schedule.dayOfMonth() < 1 || schedule.dayOfMonth() > 31) {
                    issues.add(new ConfigIssue(path + ".schedule.day-of-month", "must be from 1 through 31"));
                }
            }
            case INTERVAL -> {
                if (schedule.intervalMinutes() < 1) {
                    issues.add(new ConfigIssue(path + ".schedule.interval-minutes", "must be positive"));
                }
            }
        }
    }

    private static void requireTime(String path, ScheduleSettings schedule, List<ConfigIssue> issues) {
        if (schedule.time() == null) {
            issues.add(new ConfigIssue(path + ".schedule.time", "is required for " + schedule.type()));
        }
    }

    private static void validateWarnings(String path, List<Integer> warnings, List<ConfigIssue> issues) {
        Set<Integer> unique = new HashSet<>();
        int previous = Integer.MAX_VALUE;
        for (int warning : warnings) {
            if (warning <= 0) {
                issues.add(new ConfigIssue(path + ".warning-minutes", "values must be positive"));
            }
            if (!unique.add(warning)) {
                issues.add(new ConfigIssue(path + ".warning-minutes", "values must be unique"));
            }
            if (warning > previous) {
                issues.add(new ConfigIssue(path + ".warning-minutes", "values must be in descending order"));
            }
            previous = warning;
        }
    }

    private static void validateRegeneration(
            String path, RegenerationSettings regeneration, List<ConfigIssue> issues) {
        if (regeneration == null || regeneration.seedPolicy() == null) {
            issues.add(new ConfigIssue(path + ".regeneration.seed-policy", "is required"));
            return;
        }
        if (regeneration.seedPolicy() == SeedPolicy.FIXED && regeneration.fixedSeed() == null) {
            issues.add(new ConfigIssue(path + ".regeneration.fixed-seed", "is required for FIXED"));
        }
    }

    private static void validateEvacuation(
            String path,
            ManagedWorldSettings world,
            WorldCatalogView catalog,
            List<ConfigIssue> issues) {
        EvacuationSettings evacuation = world.evacuation();
        if (evacuation == null) {
            issues.add(new ConfigIssue(path + ".evacuation", "is required"));
            return;
        }
        if (!evacuation.enabled()) {
            return;
        }
        if (evacuation.destination().isBlank()) {
            issues.add(new ConfigIssue(path + ".evacuation.destination", "is required when evacuation is enabled"));
        } else if (isNamespacedWorldName(evacuation.destination()) && !catalog.allowsNamespacedWorldNames()) {
            issues.add(new ConfigIssue(
                    path + ".evacuation.destination",
                    "must be a plain Multiverse world name without ':'"));
        } else if (evacuation.destination().equalsIgnoreCase(world.multiverseWorld())) {
            issues.add(new ConfigIssue(path + ".evacuation.destination", "must be a different world"));
        } else if (!containsIgnoreCase(catalog.registeredWorldNames(), evacuation.destination())) {
            issues.add(new ConfigIssue(path + ".evacuation.destination", notRegisteredMessage(catalog)));
        }
    }

    private static void validateTeleport(TeleportSettings teleport, WorldCatalogView catalog, List<ConfigIssue> issues) {
        Set<String> names = new HashSet<>();
        teleport.worlds().forEach((worldName, destination) -> {
            String path = "teleport.worlds." + worldName;
            if (worldName.isBlank()) {
                issues.add(new ConfigIssue(path, "world name must not be blank"));
            } else if (isNamespacedWorldName(worldName) && !catalog.allowsNamespacedWorldNames()) {
                issues.add(new ConfigIssue(path, "must use a plain Multiverse world name without ':'"));
            }
            if (!names.add(worldName.toLowerCase(Locale.ROOT))) {
                issues.add(new ConfigIssue(path, "duplicates another teleport override ignoring case"));
            }
            String permission = destination.permission();
            if (permission != null && permission.chars().anyMatch(Character::isWhitespace)) {
                issues.add(new ConfigIssue(path + ".permission", "must not contain whitespace"));
            }
        });
    }

    static boolean isProtected(String worldName, PluginSettings settings, WorldCatalogView catalog) {
        return worldName.equalsIgnoreCase(settings.defaultHubWorld())
                || worldName.equalsIgnoreCase(catalog.defaultWorldName());
    }

    static boolean containsIgnoreCase(Set<String> values, String expected) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(expected));
    }

    private static String notRegisteredMessage(WorldCatalogView catalog) {
        return catalog.allowsNamespacedWorldNames()
                ? "is not a registered Worlds/Bukkit world"
                : "is not registered in Multiverse-Core";
    }

    private static boolean isNamespacedWorldName(String value) {
        return value.indexOf(':') >= 0;
    }
}
