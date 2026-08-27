package com.lozaine.resourceworldresetter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigRepositoryTest {
    private static final WorldCatalogView CATALOG = new WorldCatalogView() {
        @Override
        public Set<String> registeredWorldNames() {
            return Set.of("world", "resource");
        }

        @Override
        public String defaultWorldName() {
            return "world";
        }
    };

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsStableIdSeparatelyFromMultiverseName() throws Exception {
        ConfigLoadResult result = load(validConfig("resource", true, true));

        assertThat(result.valid()).isTrue();
        ManagedWorldSettings world = result.settings().world("mining-id").orElseThrow();
        assertThat(world.id()).isEqualTo("mining-id");
        assertThat(world.multiverseWorld()).isEqualTo("resource");
        assertThat(world.state()).isEqualTo(WorldOperationalState.MANAGED);
        assertThat(world.canReset()).isTrue();
        assertThat(world.regeneration().fixedSeed()).isNull();
    }

    @Test
    void missingManagedDefaultsToFalse() throws Exception {
        String config = validConfig("resource", true, true).replace("    managed: true\n", "");
        ConfigLoadResult result = load(config);

        assertThat(result.valid()).isTrue();
        ManagedWorldSettings world = result.settings().world("mining-id").orElseThrow();
        assertThat(world.managed()).isFalse();
        assertThat(world.state()).isEqualTo(WorldOperationalState.DISABLED);
        assertThat(world.canReset()).isFalse();
    }

    @Test
    void missingMultiverseWorldIsRetainedAsOrphaned() throws Exception {
        ConfigLoadResult result = load(validConfig("missing-resource", true, true));

        assertThat(result.valid()).isTrue();
        ManagedWorldSettings world = result.settings().world("mining-id").orElseThrow();
        assertThat(world.state()).isEqualTo(WorldOperationalState.ORPHANED);
        assertThat(world.canReset()).isFalse();
    }

    @Test
    void defaultAndHubWorldCannotBecomeManaged() throws Exception {
        ConfigLoadResult result = load(validConfig("world", true, true));

        assertThat(result.status()).isEqualTo(ConfigLoadStatus.INVALID);
        assertThat(result.issues()).anyMatch(issue ->
                issue.path().equals("worlds.mining-id.managed") && issue.message().contains("cannot be managed"));
    }

    @Test
    void namespacedMultiverseWorldIsRejectedOnSpigotBuild() throws Exception {
        ConfigLoadResult result = load(validConfig("minecraft:resource", true, true));

        assertThat(result.status()).isEqualTo(ConfigLoadStatus.INVALID);
        assertThat(result.issues()).anyMatch(issue ->
                issue.path().equals("worlds.mining-id.multiverse-world")
                        && issue.message().contains("plain Multiverse world name"));
    }

    @Test
    void namespacedTeleportOverrideIsRejectedOnSpigotBuild() throws Exception {
        String config = validConfig("resource", true, true).replace(
                "  worlds: {}\n",
                """
                  worlds:
                    minecraft:resource:
                      enabled: true
                      display-name: Resource
                      permission: ""
                """);

        ConfigLoadResult result = load(config);

        assertThat(result.status()).isEqualTo(ConfigLoadStatus.INVALID);
        assertThat(result.issues()).anyMatch(issue ->
                issue.path().equals("teleport.worlds.minecraft:resource")
                        && issue.message().contains("plain Multiverse world name"));
    }

    @Test
    void hubBukkitNameIsAcceptedWhenCatalogHasNamespacedIdentityAndFolderName() throws Exception {
        WorldCatalogView paperCatalog = new WorldCatalogView() {
            @Override
            public Set<String> registeredWorldNames() {
                return Set.of("minecraft:overworld", "world", "minecraft:the_nether", "world_nether");
            }

            @Override
            public String defaultWorldName() {
                return "world";
            }

            @Override
            public boolean allowsNamespacedWorldNames() {
                return true;
            }
        };
        Path configFile = write("""
                config-version: 5
                timezone: Asia/Kuala_Lumpur
                default-hub-world: world
                reset-policy:
                  max-safe-retries: 2
                  retry-delay-seconds: 30
                  broadcast-completion: true
                worlds: {}
                teleport:
                  auto-discover: true
                  default-enabled: false
                  show-locked: true
                  worlds: {}
                """);

        ConfigLoadResult result = new ConfigRepository(configFile, paperCatalog).load();

        assertThat(result.valid()).isTrue();
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void namespacedWorldKeyIsAcceptedWhenCatalogAllowsIt() throws Exception {
        WorldCatalogView paperCatalog = new WorldCatalogView() {
            @Override
            public Set<String> registeredWorldNames() {
                return Set.of("world", "worlds:resource", "resource");
            }

            @Override
            public String defaultWorldName() {
                return "world";
            }

            @Override
            public boolean allowsNamespacedWorldNames() {
                return true;
            }
        };

        ConfigLoadResult result = new ConfigRepository(write(validConfig("worlds:resource", true, true)), paperCatalog)
                .load();

        assertThat(result.valid()).isTrue();
        assertThat(result.settings().world("mining-id").orElseThrow().multiverseWorld()).isEqualTo("worlds:resource");
    }

    @Test
    void detectsV4InsteadOfInterpretingItAsV5() throws Exception {
        ConfigLoadResult result = load("worldName: resource\nworldSeed: 42\n");

        assertThat(result.status()).isEqualTo(ConfigLoadStatus.MIGRATION_REQUIRED);
        assertThat(result.settings()).isNull();
    }

    @Test
    void invalidCandidateCannotOverwriteValidFile() throws Exception {
        Path configFile = write(validConfig("resource", true, true));
        ConfigRepository repository = new ConfigRepository(configFile, CATALOG);
        PluginSettings valid = repository.load().settings();
        ManagedWorldSettings original = valid.world("mining-id").orElseThrow();
        ManagedWorldSettings protectedWorld = new ManagedWorldSettings(
                original.id(),
                "world",
                original.displayName(),
                true,
                true,
                original.schedule(),
                original.warnings(),
                original.regeneration(),
                original.evacuation(),
                WorldOperationalState.PROTECTED);
        PluginSettings invalid = new PluginSettings(
                5,
                valid.timezone(),
                valid.defaultHubWorld(),
                valid.resetPolicy(),
                java.util.Map.of(protectedWorld.id(), protectedWorld),
                valid.teleport());
        String before = Files.readString(configFile);

        assertThatThrownBy(() -> repository.save(invalid)).isInstanceOf(ConfigValidationException.class);
        assertThat(Files.readString(configFile)).isEqualTo(before);
    }

    @Test
    void saveUsesCompleteRoundTrippableSnapshot() throws Exception {
        Path configFile = write(validConfig("resource", true, true));
        ConfigRepository repository = new ConfigRepository(configFile, CATALOG);
        PluginSettings settings = repository.load().settings();

        repository.save(settings);

        ConfigLoadResult reloaded = repository.load();
        assertThat(reloaded.valid()).isTrue();
        assertThat(reloaded.settings()).isEqualTo(settings);
        try (var files = Files.list(temporaryDirectory)) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith(".tmp"))).isEmpty();
        }
    }

    @Test
    void migratesLegacySecondWarningsAndRemovesTeleportDisplayNameOnSave() throws Exception {
        String config = validConfig("resource", true, true).replace(
                "  worlds: {}\n",
                """
                  worlds:
                    resource:
                      enabled: true
                      display-name: Legacy duplicate
                      permission: rwr.teleport.resource
                """);
        Path configFile = write(config);
        ConfigRepository repository = new ConfigRepository(configFile, CATALOG);
        PluginSettings settings = repository.load().settings();

        assertThat(settings.world("mining-id").orElseThrow().warnings())
                .containsExactly(30, 10, 1);
        repository.save(settings);

        String saved = Files.readString(configFile);
        assertThat(saved).contains("warning-minutes:");
        assertThat(saved).doesNotContain("warnings:");
        assertThat(saved).doesNotContain("Legacy duplicate");
        assertThat(saved).contains("permission: rwr.teleport.resource");
    }

    private ConfigLoadResult load(String content) throws Exception {
        return new ConfigRepository(write(content), CATALOG).load();
    }

    private Path write(String content) throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, content);
        return configFile;
    }

    private static String validConfig(String multiverseWorld, boolean enabled, boolean managed) {
        return """
                config-version: 5
                timezone: Asia/Kuala_Lumpur
                default-hub-world: world
                reset-policy:
                  max-safe-retries: 2
                  retry-delay-seconds: 30
                  broadcast-completion: true
                worlds:
                  mining-id:
                    multiverse-world: %s
                    display-name: Resource World
                    enabled: %s
                    managed: %s
                    schedule:
                      type: DAILY
                      time: "03:00"
                    warnings: [1800, 600, 60]
                    regeneration:
                      seed-policy: RANDOM
                      fixed-seed: 0
                      keep-world-config: true
                      keep-gamerules: true
                      keep-world-border: true
                    evacuation:
                      enabled: true
                      destination: world
                teleport:
                  auto-discover: true
                  default-enabled: false
                  show-locked: true
                  worlds: {}
                """.formatted(multiverseWorld, enabled, managed);
    }
}
