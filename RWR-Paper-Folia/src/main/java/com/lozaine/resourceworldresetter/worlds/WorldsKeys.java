package com.lozaine.resourceworldresetter.worlds;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.Level;
import net.thenextlvl.worlds.WorldRegistry;
import net.thenextlvl.worlds.WorldsAccess;
import org.bukkit.Bukkit;
import org.bukkit.World;

/** Shared Worlds key parsing and Bukkit world resolution. */
public final class WorldsKeys {
    private final WorldsAccess access;
    private final Logger logger;

    public WorldsKeys(WorldsAccess access, Logger logger) {
        this.access = Objects.requireNonNull(access, "access");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public Key parseKey(String raw) {
        Objects.requireNonNull(raw, "key");
        raw = raw.trim();
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            return Key.key(parts[0], parts[1]);
        }
        return Key.key("minecraft", raw);
    }

    /**
     * Resolve the authoritative Worlds key for a loaded Bukkit world.
     * Order: Level.copy metadata, WorldRegistry match, Paper world key, parsed Bukkit name.
     */
    public Key resolveKey(World world) {
        Objects.requireNonNull(world, "world");

        try {
            Key fromLevel = Level.copy(world).key();
            if (fromLevel != null) {
                return fromLevel;
            }
        } catch (Throwable throwable) {
            logger.fine("Level.copy key resolve failed for " + world.getName() + ": " + throwable.getMessage());
        }

        String name = world.getName();
        try {
            WorldRegistry registry = access.getWorldRegistry();
            Optional<Key> matched = registry.entrySet()
                    .filter(entry -> keyMatchesName(entry.getKey(), name))
                    .map(Map.Entry::getKey)
                    .findFirst();
            if (matched.isPresent()) {
                return matched.get();
            }
        } catch (Throwable throwable) {
            logger.fine("WorldRegistry key resolve failed for " + name + ": " + throwable.getMessage());
        }

        try {
            if (world instanceof net.kyori.adventure.key.Keyed keyed) {
                Key paperKey = keyed.key();
                if (paperKey != null) {
                    return paperKey;
                }
            }
        } catch (Throwable ignored) {
            // older API surface
        }
        try {
            org.bukkit.NamespacedKey namespacedKey = world.getKey();
            if (namespacedKey != null) {
                return Key.key(namespacedKey.getNamespace(), namespacedKey.getKey());
            }
        } catch (Throwable ignored) {
            // ignore
        }

        return parseKey(name);
    }

    public boolean keyRefersTo(String configKey, World world) {
        if (configKey == null || world == null) {
            return false;
        }
        Key expected = parseKey(configKey);
        Key actual = resolveKey(world);
        if (expected.equals(actual)) {
            return true;
        }
        return keyMatchesName(expected, world.getName());
    }

    public Optional<World> getWorld(Key key) {
        World world = Bukkit.getWorld(key.asString());
        if (world != null) {
            return Optional.of(world);
        }

        String sanitized = key.asString().replace(':', '_');
        world = Bukkit.getWorld(sanitized);
        if (world != null) {
            return Optional.of(world);
        }

        if ("minecraft".equals(key.namespace())) {
            world = Bukkit.getWorld(key.value());
            if (world != null) {
                return Optional.of(world);
            }
        }

        world = Bukkit.getWorld(key.value());
        if (world != null) {
            return Optional.of(world);
        }
        return Optional.empty();
    }

    public Optional<World> getWorld(String rawKey) {
        return getWorld(parseKey(rawKey));
    }

    public static boolean keyMatchesName(Key key, String bukkitName) {
        if (key == null || bukkitName == null) {
            return false;
        }
        return bukkitName.equalsIgnoreCase(key.asString())
                || bukkitName.equalsIgnoreCase(key.asString().replace(':', '_'))
                || bukkitName.equalsIgnoreCase(key.value());
    }
}
