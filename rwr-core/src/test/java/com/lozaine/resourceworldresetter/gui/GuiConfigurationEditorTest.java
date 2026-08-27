package com.lozaine.resourceworldresetter.gui;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.config.ConfigRepository;
import com.lozaine.resourceworldresetter.config.ConfigService;
import com.lozaine.resourceworldresetter.config.EvacuationSettings;
import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.config.RegenerationSettings;
import com.lozaine.resourceworldresetter.config.ResetPolicySettings;
import com.lozaine.resourceworldresetter.config.ScheduleSettings;
import com.lozaine.resourceworldresetter.config.ScheduleType;
import com.lozaine.resourceworldresetter.config.TeleportDestinationSettings;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import com.lozaine.resourceworldresetter.multiverse.RegenerationOutcome;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRequest;
import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GuiConfigurationEditorTest {
    @TempDir
    Path directory;

    private Path configFile;
    private FakeGateway gateway;
    private ConfigService configs;
    private GuiConfigurationEditor editor;

    @BeforeEach
    void setUp() throws Exception {
        configFile = directory.resolve("config.yml");
        Files.writeString(configFile, emptyConfig());
        gateway = new FakeGateway();
        configs = new ConfigService(new ConfigRepository(configFile, gateway));
        assertThat(configs.reload().accepted()).isTrue();
        editor = new GuiConfigurationEditor(configs, gateway);
    }

    @Test
    void addConfiguresExistingMultiverseWorldWithoutCallingLifecycleOperations() {
        assertThat(editor.addWorld("resource").accepted()).isTrue();

        ManagedWorldSettings added = configs.current().world("resource").orElseThrow();
        assertThat(added.multiverseWorld()).isEqualTo("resource");
        assertThat(added.enabled()).isFalse();
        assertThat(added.managed()).isTrue();
        assertThat(gateway.regenerationCalls).hasValue(0);
        assertThat(editor.addWorld("resource").accepted()).isFalse();
        assertThat(editor.addWorld("world").accepted()).isFalse();
    }

    @Test
    void everySupportedWorldSettingIsValidatedSavedAndReloaded() {
        assertThat(editor.addWorld("resource").accepted()).isTrue();
        GuiEditResult result = editor.updateWorld("resource", old -> GuiConfigurationEditor.copyWorld(
                old,
                "Mining World",
                true,
                new ScheduleSettings(ScheduleType.WEEKLY, LocalTime.of(4, 30), DayOfWeek.FRIDAY, 0, 0),
                List.of(30, 10, 1),
                new RegenerationSettings(SeedPolicy.FIXED, 12345L, false, false, false),
                new EvacuationSettings(true, "hub")), "saved");

        assertThat(result.accepted()).isTrue();
        ConfigService reopened = new ConfigService(new ConfigRepository(configFile, gateway));
        assertThat(reopened.reload().accepted()).isTrue();
        ManagedWorldSettings persisted = reopened.current().world("resource").orElseThrow();
        assertThat(persisted.displayName()).isEqualTo("Mining World");
        assertThat(persisted.enabled()).isTrue();
        assertThat(persisted.schedule().type()).isEqualTo(ScheduleType.WEEKLY);
        assertThat(persisted.schedule().dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(persisted.warnings()).containsExactly(30, 10, 1);
        assertThat(persisted.regeneration().seedPolicy()).isEqualTo(SeedPolicy.FIXED);
        assertThat(persisted.regeneration().fixedSeed()).isEqualTo(12345L);
        assertThat(persisted.regeneration().keepWorldConfig()).isFalse();
        assertThat(persisted.regeneration().keepGameRules()).isFalse();
        assertThat(persisted.regeneration().keepWorldBorder()).isFalse();
        assertThat(persisted.evacuation().destination()).isEqualTo("hub");
    }

    @Test
    void invalidEditDoesNotPersistOrNotifyRescheduler() {
        assertThat(editor.addWorld("resource").accepted()).isTrue();
        AtomicInteger reschedules = new AtomicInteger();
        configs.addChangeListener(ignored -> reschedules.incrementAndGet());

        GuiEditResult rejected = editor.updateWorld("resource", old -> GuiConfigurationEditor.copyWorld(
                old, "", null, null, null, null, null), "saved");

        assertThat(rejected.accepted()).isFalse();
        assertThat(configs.current().world("resource").orElseThrow().displayName()).isEqualTo("resource");
        assertThat(reschedules).hasValue(0);
    }

    @Test
    void globalAndTeleportSettingsAreEditableWithoutYaml() {
        assertThat(editor.setTimezone(ZoneId.of("Europe/London")).accepted()).isTrue();
        assertThat(editor.setHub("hub").accepted()).isTrue();
        assertThat(editor.updateResetPolicy(old -> new ResetPolicySettings(4, 90, false)).accepted()).isTrue();
        assertThat(editor.updateTeleport(old -> new TeleportSettings(false, true, false, old.worlds()), "saved")
                .accepted()).isTrue();
        assertThat(editor.updateTeleportWorld("resource", old ->
                        new TeleportDestinationSettings(true, "rwr.teleport.resource"), "saved")
                .accepted()).isTrue();

        ConfigService reopened = new ConfigService(new ConfigRepository(configFile, gateway));
        assertThat(reopened.reload().accepted()).isTrue();
        assertThat(reopened.current().timezone()).isEqualTo(ZoneId.of("Europe/London"));
        assertThat(reopened.current().defaultHubWorld()).isEqualTo("hub");
        assertThat(reopened.current().resetPolicy()).isEqualTo(new ResetPolicySettings(4, 90, false));
        assertThat(reopened.current().teleport().autoDiscover()).isFalse();
        assertThat(reopened.current().teleport().defaultEnabled()).isTrue();
        assertThat(reopened.current().teleport().showLocked()).isFalse();
        assertThat(reopened.current().teleport().worlds().get("resource").permission())
                .isEqualTo("rwr.teleport.resource");
    }

    @Test
    void removalDeletesOnlyRwrConfigurationAndNeverTheMultiverseWorld() {
        assertThat(editor.addWorld("resource").accepted()).isTrue();
        GuiEditResult removed = editor.removeWorld("resource");

        assertThat(removed.accepted()).isTrue();
        assertThat(removed.message()).contains("Multiverse world was not deleted");
        assertThat(configs.current().worlds()).isEmpty();
        assertThat(gateway.world("resource")).isPresent();
        assertThat(gateway.regenerationCalls).hasValue(0);
    }

    @Test
    void warningParserOrdersAndDeduplicatesBeforeValidation() {
        assertThat(GuiConfigurationEditor.parseWarnings("1, 30, 10, 1"))
                .containsExactly(30, 10, 1);
        assertThat(GuiConfigurationEditor.parseWarnings("none")).isEmpty();
    }

    @Test
    void newlyManagedWorldsPreserveLeftToRightInsertionOrder() {
        assertThat(editor.addWorld("resource").accepted()).isTrue();
        assertThat(editor.addWorld("rainforest").accepted()).isTrue();

        assertThat(configs.current().worlds().keySet()).containsExactly("resource", "rainforest");
    }

    private static String emptyConfig() {
        return """
                config-version: 5
                timezone: UTC
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
                """;
    }

    private static final class FakeGateway implements WorldProvider {
        private final List<WorldSnapshot> worlds = List.of(
                snapshot("world"), snapshot("hub"), snapshot("resource"), snapshot("rainforest"));
        private final AtomicInteger regenerationCalls = new AtomicInteger();

        @Override
        public String providerName() {
            return "Multiverse";
        }

        @Override
        public List<WorldSnapshot> registeredWorlds() {
            return worlds;
        }

        @Override
        public List<WorldSnapshot> loadedWorlds() {
            return worlds;
        }

        @Override
        public Optional<WorldSnapshot> world(String name) {
            return worlds.stream().filter(world -> world.name().equalsIgnoreCase(name)).findFirst();
        }

        @Override
        public DestinationResult resolveSafeDestination(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RegenerationOutcome regenerate(RegenerationRequest request) {
            regenerationCalls.incrementAndGet();
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> registeredWorldNames() {
            return Set.of("world", "hub", "resource", "rainforest");
        }

        @Override
        public String defaultWorldName() {
            return "world";
        }

        private static WorldSnapshot snapshot(String name) {
            return new WorldSnapshot(name, name, name, true, "NORMAL", 1L, "", "", "NORMAL", true, true, "0,64,0");
        }
    }
}
