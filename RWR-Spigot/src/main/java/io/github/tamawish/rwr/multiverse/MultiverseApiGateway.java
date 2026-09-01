package io.github.tamawish.rwr.multiverse;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import io.github.tamawish.rwr.world.BukkitLocations;
import io.github.tamawish.rwr.world.WorldProvider;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.event.world.MVWorldRegeneratedEvent;
import org.mvplugins.multiverse.core.teleportation.BlockSafety;
import org.mvplugins.multiverse.core.utils.compatibility.WorldCompatibility;
import org.mvplugins.multiverse.core.utils.result.Attempt;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.core.world.helpers.DataStore;
import org.mvplugins.multiverse.core.world.helpers.DataTransfer;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.DeleteWorldOptions;
import org.mvplugins.multiverse.core.world.options.RegenWorldOptions;
import org.mvplugins.multiverse.external.vavr.control.Option;

public final class MultiverseApiGateway implements WorldProvider {
    private static final Pattern SPIGOT_WORLD_NAME = Pattern.compile("[A-Za-z0-9_.-]+");
    private final WorldManager worldManager;
    private final BlockSafety blockSafety;
    private final RegenerationOptionsMapper optionsMapper;

    public MultiverseApiGateway(MultiverseCoreApi api) {
        this(api.getWorldManager(), api.getBlockSafety(), new RegenerationOptionsMapper());
    }

    MultiverseApiGateway(
            WorldManager worldManager,
            BlockSafety blockSafety,
            RegenerationOptionsMapper optionsMapper) {
        this.worldManager = worldManager;
        this.blockSafety = blockSafety;
        this.optionsMapper = optionsMapper;
    }

    @Override
    public String providerName() {
        return "Multiverse";
    }

    @Override
    public List<WorldSnapshot> registeredWorlds() {
        return worldManager.getWorlds().stream()
                .map(this::snapshot)
                .sorted(Comparator.comparing(WorldSnapshot::name))
                .toList();
    }

    @Override
    public List<WorldSnapshot> loadedWorlds() {
        return worldManager.getWorlds().stream()
                .filter(LoadedMultiverseWorld.class::isInstance)
                .map(this::snapshot)
                .sorted(Comparator.comparing(WorldSnapshot::name))
                .toList();
    }

    @Override
    public Optional<WorldSnapshot> world(String name) {
        return worldManager.getWorld(name).toJavaOptional().map(this::snapshot);
    }

    @Override
    public Set<String> registeredWorldNames() {
        return worldManager.getWorlds().stream()
                .map(MultiverseWorld::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String defaultWorldName() {
        return worldManager.getDefaultWorld()
                .map(MultiverseWorld::getName)
                .get();
    }

    @Override
    public DestinationResult resolveSafeDestination(String name) {
        if (worldManager.getWorld(name).isEmpty()) {
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.NOT_REGISTERED,
                    "World is not registered in Multiverse-Core.");
        }
        Option<LoadedMultiverseWorld> loadedOption = worldManager.getLoadedWorld(name);
        if (loadedOption.isEmpty()) {
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.NOT_LOADED,
                    "World is registered in Multiverse-Core but is not loaded.");
        }

        Location configuredSpawn = loadedOption.get().getSpawnLocation();
        if (configuredSpawn.getWorld() == null) {
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.INVALID_SPAWN_WORLD,
                    "Multiverse spawn has no loaded Bukkit world.");
        }
        if (blockSafety.canSpawnAtLocationSafely(configuredSpawn)) {
            return new DestinationResult.Available(BukkitLocations.from(configuredSpawn), false);
        }

        Location adjusted = blockSafety.findSafeSpawnLocation(configuredSpawn);
        if (adjusted == null || adjusted.getWorld() == null) {
            return new DestinationResult.Unavailable(
                    DestinationFailureReason.NO_SAFE_LOCATION,
                    "Multiverse could not resolve a safe spawn location.");
        }
        return new DestinationResult.Available(BukkitLocations.from(adjusted), true);
    }

    @Override
    public RegenerationOutcome regenerate(RegenerationRequest request) {
        Option<MultiverseWorld> registeredOption = worldManager.getWorld(request.worldName());
        if (registeredOption.isEmpty()) {
            return new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.NOT_REGISTERED,
                    "World is not registered in Multiverse-Core.");
        }

        Option<LoadedMultiverseWorld> loadedOption = worldManager.getLoadedWorld(request.worldName());
        if (loadedOption.isEmpty()) {
            return new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.NOT_LOADED,
                    "World is registered but not loaded.");
        }
        LoadedMultiverseWorld world = loadedOption.get();

        if (worldManager.getDefaultWorld()
                .exists(defaultWorld -> defaultWorld.getName().equalsIgnoreCase(world.getName()))) {
            return new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.PROTECTED_DEFAULT_WORLD,
                    "The server default world is protected.");
        }
        if (world.getPlayers().exists(players -> !players.isEmpty())) {
            return new RegenerationOutcome.Rejected(
                    RegenerationRejectionReason.PLAYERS_PRESENT,
                    "The world still contains players.");
        }

        try {
            RegenWorldOptions options = optionsMapper.map(world, request);
            return regenerateUsingMultiverseLegacyName(world, options);
        } catch (RuntimeException exception) {
            return new RegenerationOutcome.Failed(
                    RegenerationFailureReason.API_EXCEPTION,
                    exception.getClass().getSimpleName(),
                    nullToEmpty(exception.getMessage()));
        }
    }

    /**
     * Reproduces Multiverse's regeneration transaction with its public API, but feeds creation the
     * legacy name supplied by the Multiverse world itself. Multiverse 5.8 otherwise reuses the
     * stored namespaced key during regen; Spigot cannot construct a world from that key and only
     * reports the problem after the old world has already been deleted.
     */
    private RegenerationOutcome regenerateUsingMultiverseLegacyName(
            LoadedMultiverseWorld world, RegenWorldOptions options) {
        DataTransfer<LoadedMultiverseWorld> transfer = new DataTransfer<>();
        if (options.keepWorldConfig()) {
            transfer.addDataStore(new DataStore.WorldConfigStore<>(), world);
        }
        if (options.keepGameRule()) {
            transfer.addDataStore(new DataStore.GameRulesStore(), world);
        }
        if (options.keepWorldBorder()) {
            transfer.addDataStore(new DataStore.WorldBorderStore(), world);
        }

        boolean restoreSpawn = options.keepWorldConfig() && options.seed() == world.getSeed();
        Location spawn = world.getSpawnLocation();
        // Resolve and validate this before deletion. The loaded Bukkit name is the real Spigot
        // folder name even when Multiverse exposes a namespaced registry identity.
        String createName = resolveSpigotSafeWorldName(world);
        CreateWorldOptions create = CreateWorldOptions.worldName(createName)
                .biome(world.getBiome())
                .bonusChest(world.getBukkitWorld().map(WorldCompatibility::hasBonusChest).getOrElse(false))
                .environment(world.getEnvironment())
                .generateStructures(world.canGenerateStructures().getOrElse(true))
                .generator(world.getGenerator())
                .generatorSettings(world.getGeneratorSettings())
                .seed(options.seed())
                .useSpawnAdjust(!restoreSpawn && world.getAdjustSpawn())
                .worldType(world.getWorldType().getOrElse(org.bukkit.WorldType.NORMAL))
                .doFolderCheck(options.keepFiles().isEmpty());

        Attempt<String, org.mvplugins.multiverse.core.world.reasons.DeleteFailureReason> deletion =
                worldManager.deleteWorld(DeleteWorldOptions.world(world).keepFiles(options.keepFiles()));
        if (deletion.isFailure()) {
            return new RegenerationOutcome.Failed(
                    RegenerationFailureReason.DELETE_FAILED,
                    deletion.getFailureReason().name(),
                    MultiverseFailureMessages.format(deletion.getFailureMessage()));
        }

        Attempt<LoadedMultiverseWorld, org.mvplugins.multiverse.core.world.reasons.CreateFailureReason> creation =
                worldManager.createWorld(create);
        if (creation.isFailure()) {
            return new RegenerationOutcome.Failed(
                    RegenerationFailureReason.CREATE_FAILED,
                    creation.getFailureReason().name(),
                    MultiverseFailureMessages.format(creation.getFailureMessage()));
        }

        LoadedMultiverseWorld recreated = creation.get();
        transfer.pasteAllTo(recreated);
        if (restoreSpawn) {
            recreated.setSpawnLocation(spawn);
        }
        worldManager.saveWorldsConfig();
        Bukkit.getPluginManager().callEvent(new MVWorldRegeneratedEvent(recreated));
        return new RegenerationOutcome.Success(snapshot(recreated));
    }

    static String resolveSpigotSafeWorldName(LoadedMultiverseWorld world) {
        String bukkitName = world.getBukkitWorld().map(World::getName).getOrNull();
        String legacyName = world.getName();
        String keyPath = world.getKey().getKey();
        return resolveSpigotSafeWorldName(bukkitName, legacyName, keyPath, world.getKey().toString());
    }

    static String resolveSpigotSafeWorldName(
            String bukkitName, String legacyName, String keyPath, String multiverseIdentity) {
        if (isSpigotWorldName(bukkitName)) {
            return bukkitName;
        }
        if (isSpigotWorldName(legacyName)) {
            return legacyName;
        }
        if (isSpigotWorldName(keyPath)) {
            return keyPath;
        }

        String reported = firstNonBlank(legacyName, keyPath, multiverseIdentity);
        String stripped = stripNamespace(reported);
        if (isSpigotWorldName(stripped)) {
            return stripped;
        }
        throw new IllegalStateException(
                "Cannot resolve a Spigot-safe world name from Multiverse identity '"
                        + nullToEmpty(multiverseIdentity)
                        + "'.");
    }

    private static boolean isSpigotWorldName(String value) {
        return value != null && SPIGOT_WORLD_NAME.matcher(value).matches();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String stripNamespace(String value) {
        int colon = value.lastIndexOf(':');
        return colon < 0 ? value : value.substring(colon + 1);
    }

    private WorldSnapshot snapshot(MultiverseWorld world) {
        boolean loaded = world instanceof LoadedMultiverseWorld;
        String worldType = "UNAVAILABLE";
        boolean structures = false;
        if (world instanceof LoadedMultiverseWorld loadedWorld) {
            worldType = loadedWorld.getWorldType().map(Enum::name).getOrElse("UNKNOWN");
            structures = loadedWorld.canGenerateStructures().getOrElse(false);
        }
        Location spawn = world.getSpawnLocation();
        return new WorldSnapshot(
                world.getKey().toString(),
                world.getName(),
                world.getAlias(),
                loaded,
                world.getEnvironment().name(),
                world.getSeed(),
                nullToEmpty(world.getGenerator()),
                nullToEmpty(world.getGeneratorSettings()),
                worldType,
                structures,
                world.getAdjustSpawn(),
                format(spawn));
    }

    private static String format(Location location) {
        World world = location.getWorld();
        String worldName = world == null ? "unloaded" : world.getName();
        return "%s %.1f %.1f %.1f".formatted(worldName, location.getX(), location.getY(), location.getZ());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
