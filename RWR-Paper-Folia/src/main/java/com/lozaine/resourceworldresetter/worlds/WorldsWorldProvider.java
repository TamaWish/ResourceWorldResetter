package com.lozaine.resourceworldresetter.worlds;

import com.lozaine.resourceworldresetter.multiverse.DestinationFailureReason;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.multiverse.RegenerationFailureReason;
import com.lozaine.resourceworldresetter.multiverse.RegenerationOutcome;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRejectionReason;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRequest;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import com.lozaine.resourceworldresetter.world.BukkitLocations;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.WorldsAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Worlds-backed {@link WorldProvider}.
 *
 * <p>Worlds regeneration is exposed asynchronously so Paper's primary thread and Folia's global
 * region never block waiting for work that must resume on those schedulers.
 */
public final class WorldsWorldProvider implements WorldProvider {
    private static final long REGENERATE_TIMEOUT_SECONDS = 120L;

    private final WorldsAccess access;
    private final WorldsKeys keys;
    private final Logger logger;
    private final Plugin plugin;

    public WorldsWorldProvider(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.access = WorldsAccess.access();
        if (this.access == null) {
            throw new IllegalStateException("WorldsAccess.access() returned null – is Worlds installed and enabled?");
        }
        this.keys = new WorldsKeys(access, logger);
        logger.info("WorldsAccess acquired successfully (Worlds API).");
    }

    public WorldsKeys keys() {
        return keys;
    }

    public WorldsAccess access() {
        return access;
    }

    @Override
    public String providerName() {
        return "Worlds";
    }

    @Override
    public List<WorldSnapshot> registeredWorlds() {
        Map<String, WorldSnapshot> byIdentity = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            WorldSnapshot snapshot = snapshot(world);
            byIdentity.put(normalize(snapshot.identity()), snapshot);
        }
        try {
            access.getWorldRegistry().entrySet().forEach(entry -> {
                Key key = entry.getKey();
                String identity = key.asString();
                byIdentity.computeIfAbsent(normalize(identity), ignored -> unloadedSnapshot(key));
            });
        } catch (Throwable throwable) {
            logger.fine("Could not enumerate Worlds registry: " + throwable.getMessage());
        }
        return byIdentity.values().stream()
                .sorted(Comparator.comparing(WorldSnapshot::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public List<WorldSnapshot> loadedWorlds() {
        return Bukkit.getWorlds().stream()
                .map(this::snapshot)
                .sorted(Comparator.comparing(WorldSnapshot::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<WorldSnapshot> world(String name) {
        Optional<World> loaded = keys.getWorld(name);
        if (loaded.isPresent()) {
            return Optional.of(snapshot(loaded.get()));
        }
        Key key = keys.parseKey(name);
        try {
            boolean registered = access.getWorldRegistry().entrySet()
                    .anyMatch(entry -> entry.getKey().equals(key)
                            || entry.getKey().asString().equalsIgnoreCase(name)
                            || WorldsKeys.keyMatchesName(entry.getKey(), name));
            if (registered) {
                return Optional.of(unloadedSnapshot(key));
            }
        } catch (Throwable throwable) {
            logger.fine("World registry lookup failed for " + name + ": " + throwable.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Set<String> registeredWorldNames() {
        Set<String> names = new LinkedHashSet<>();
        for (WorldSnapshot snapshot : registeredWorlds()) {
            addCatalogName(names, snapshot.identity());
            addCatalogName(names, snapshot.name());
            addCatalogName(names, snapshot.alias());
        }
        return Set.copyOf(names);
    }

    @Override
    public boolean allowsNamespacedWorldNames() {
        return true;
    }

    @Override
    public String defaultWorldName() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            return "world";
        }
        return worlds.getFirst().getName();
    }

    @Override
    public DestinationResult resolveSafeDestination(String name) {
        Optional<World> loaded = keys.getWorld(name);
        if (loaded.isEmpty()) {
            Optional<WorldSnapshot> registered = world(name);
            if (registered.isEmpty()) {
                return new DestinationResult.Unavailable(
                        DestinationFailureReason.NOT_REGISTERED,
                        "World is not registered in Worlds / Bukkit.");
            }
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.NOT_LOADED,
                    "World is registered but not loaded.");
        }
        Location spawn = loaded.get().getSpawnLocation();
        if (spawn.getWorld() == null) {
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.INVALID_SPAWN_WORLD,
                    "World spawn has no loaded Bukkit world.");
        }
        return new DestinationResult.Available(BukkitLocations.from(spawn), false);
    }

    @Override
    public RegenerationOutcome regenerate(RegenerationRequest request) {
        return new RegenerationOutcome.Failed(
                RegenerationFailureReason.API_EXCEPTION,
                "ASYNC_REQUIRED",
                "Worlds regeneration must be invoked through regenerateAsync.");
    }

    /**
     * Starts {@link WorldsAccess#regenerate(World)} and completes on the global scheduler.
     * Seed / keep-* options are accepted for schema compatibility; Worlds does not expose
     * equivalent knobs in this adapter.
     */
    @Override
    public CompletionStage<RegenerationOutcome> regenerateAsync(RegenerationRequest request) {
        Optional<World> loaded = keys.getWorld(request.worldName());
        if (loaded.isEmpty()) {
            Optional<WorldSnapshot> registered = world(request.worldName());
            if (registered.isEmpty()) {
                return CompletableFuture.completedFuture(new RegenerationOutcome.Rejected(
                        RegenerationRejectionReason.NOT_REGISTERED,
                        "World is not registered in Worlds / Bukkit."));
            }
            return CompletableFuture.completedFuture(new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.NOT_LOADED,
                    "World is registered but not loaded."));
        }

        World world = loaded.get();
        if (world.getPlayers().stream().findAny().isPresent()) {
            return CompletableFuture.completedFuture(new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.PLAYERS_PRESENT,
                    "The world still contains players."));
        }

        String identity = keys.resolveKey(world).asString();
        CompletableFuture<RegenerationOutcome> result = new CompletableFuture<>();
        try {
            access.regenerate(world)
                    .orTimeout(REGENERATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .whenComplete((regenerated, error) -> plugin.getServer()
                            .getGlobalRegionScheduler()
                            .run(plugin, ignored -> result.complete(regenerationOutcome(identity, regenerated, error))));
        } catch (RuntimeException exception) {
            result.complete(regenerationOutcome(identity, null, exception));
        }
        return result;
    }

    private RegenerationOutcome regenerationOutcome(String identity, World regenerated, Throwable error) {
        if (error != null) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            if (cause instanceof java.util.concurrent.TimeoutException) {
                return new RegenerationOutcome.Failed(
                        RegenerationFailureReason.API_EXCEPTION,
                        "TIMEOUT",
                        "Worlds regenerate timed out after " + REGENERATE_TIMEOUT_SECONDS + "s for " + identity);
            }
            return new RegenerationOutcome.Failed(
                    RegenerationFailureReason.API_EXCEPTION,
                    cause.getClass().getSimpleName(),
                    nullToEmpty(cause.getMessage()));
        }
        if (regenerated == null) {
            return new RegenerationOutcome.Failed(
                    RegenerationFailureReason.API_EXCEPTION,
                    "NULL_WORLD",
                    "Worlds regenerate returned null for " + identity);
        }
        logger.info("Regenerated world via Worlds API: " + identity);
        return new RegenerationOutcome.Success(snapshot(regenerated));
    }

    private WorldSnapshot snapshot(World world) {
        Key key = keys.resolveKey(world);
        Location spawn = world.getSpawnLocation();
        return new WorldSnapshot(
                key.asString(),
                world.getName(),
                world.getName(),
                true,
                world.getEnvironment().name(),
                world.getSeed(),
                "",
                "",
                world.getWorldType() == null ? "UNKNOWN" : world.getWorldType().name(),
                world.canGenerateStructures(),
                true,
                format(spawn));
    }

    private WorldSnapshot unloadedSnapshot(Key key) {
        return new WorldSnapshot(
                key.asString(),
                key.value(),
                key.asString(),
                false,
                "UNKNOWN",
                0L,
                "",
                "",
                "UNAVAILABLE",
                false,
                false,
                "unloaded");
    }

    private static String format(Location location) {
        World world = location.getWorld();
        String worldName = world == null ? "unloaded" : world.getName();
        return "%s %.1f %.1f %.1f".formatted(worldName, location.getX(), location.getY(), location.getZ());
    }

    private static void addCatalogName(Set<String> names, String value) {
        if (value != null && !value.isBlank()) {
            names.add(value);
        }
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
