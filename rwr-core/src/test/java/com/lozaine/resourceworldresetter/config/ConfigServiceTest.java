package com.lozaine.resourceworldresetter.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void rejectedReloadRetainsPreviousCompleteSnapshot() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, emptyValidConfig("Asia/Kuala_Lumpur"));
        ConfigRepository repository = new ConfigRepository(configFile, catalog());
        ConfigService service = new ConfigService(repository);

        assertThat(service.reload().accepted()).isTrue();
        PluginSettings original = service.current();
        Files.writeString(configFile, emptyValidConfig("Not/A_Zone"));

        ConfigService.ReloadResult rejected = service.reload();
        assertThat(rejected.accepted()).isFalse();
        assertThat(rejected.retainedPrevious()).isTrue();
        assertThat(service.current()).isSameAs(original);
        assertThat(service.current().timezone().getId()).isEqualTo("Asia/Kuala_Lumpur");
    }

    @Test
    void lifecycleReconciliationChangesOnlyInMemoryOperationalState() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, managedWorldConfig());
        MutableCatalog catalog = new MutableCatalog();
        ConfigService service = new ConfigService(new ConfigRepository(configFile, catalog));
        assertThat(service.reload().accepted()).isTrue();
        AtomicInteger changes = new AtomicInteger();
        service.addChangeListener(settings -> changes.incrementAndGet());
        String persisted = Files.readString(configFile);

        catalog.names.remove("resource");
        assertThat(service.reconcileWorldStates(catalog).changedWorlds()).isEqualTo(1);
        assertThat(service.current().world("resource_world").orElseThrow().state())
                .isEqualTo(WorldOperationalState.ORPHANED);
        assertThat(Files.readString(configFile)).isEqualTo(persisted);
        assertThat(changes).hasValue(1);

        catalog.names.add("resource");
        assertThat(service.reconcileWorldStates(catalog).changedWorlds()).isEqualTo(1);
        assertThat(service.current().world("resource_world").orElseThrow().state())
                .isEqualTo(WorldOperationalState.MANAGED);
        assertThat(changes).hasValue(2);
    }

    @Test
    void acceptedReloadsAndTransactionalSavesNotifyScheduleListeners() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.yml");
        Files.writeString(configFile, emptyValidConfig("Asia/Kuala_Lumpur"));
        ConfigService service = new ConfigService(new ConfigRepository(configFile, catalog()));
        assertThat(service.reload().accepted()).isTrue();
        AtomicInteger changes = new AtomicInteger();
        ListenerRegistration registration = service.addChangeListener(settings -> changes.incrementAndGet());

        assertThat(service.saveAndApply(service.current()).accepted()).isTrue();
        assertThat(changes).hasValue(1);

        Files.writeString(configFile, emptyValidConfig("Not/A_Zone"));
        assertThat(service.reload().accepted()).isFalse();
        assertThat(changes).hasValue(1);

        registration.unregister();
        Files.writeString(configFile, emptyValidConfig("UTC"));
        assertThat(service.reload().accepted()).isTrue();
        assertThat(changes).hasValue(1);
    }

    private static WorldCatalogView catalog() {
        return new WorldCatalogView() {
            @Override
            public Set<String> registeredWorldNames() {
                return Set.of("world");
            }

            @Override
            public String defaultWorldName() {
                return "world";
            }
        };
    }

    private static String emptyValidConfig(String timezone) {
        return """
                config-version: 5
                timezone: %s
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
                """.formatted(timezone);
    }

    private static String managedWorldConfig() {
        return """
                config-version: 5
                timezone: Asia/Kuala_Lumpur
                default-hub-world: world
                reset-policy:
                  max-safe-retries: 2
                  retry-delay-seconds: 30
                  broadcast-completion: true
                worlds:
                  resource_world:
                    multiverse-world: resource
                    display-name: Resource
                    enabled: true
                    managed: true
                    schedule:
                      type: INTERVAL
                      interval-minutes: 60
                    warnings: [60, 10]
                    regeneration:
                      seed-policy: SAME
                      keep-world-config: true
                      keep-gamerules: true
                      keep-world-border: true
                    evacuation:
                      enabled: false
                teleport:
                  auto-discover: true
                  default-enabled: false
                  show-locked: true
                  worlds: {}
                """;
    }

    private static final class MutableCatalog implements WorldCatalogView {
        private final Set<String> names = new HashSet<>(Set.of("world", "resource"));

        @Override
        public Set<String> registeredWorldNames() {
            return Set.copyOf(names);
        }

        @Override
        public String defaultWorldName() {
            return "world";
        }
    }
}
