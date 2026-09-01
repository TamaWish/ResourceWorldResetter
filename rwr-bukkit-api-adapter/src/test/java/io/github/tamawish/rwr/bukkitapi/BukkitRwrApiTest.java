package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.config.ScheduleSettings;
import io.github.tamawish.rwr.config.ScheduleType;
import io.github.tamawish.rwr.config.WorldOperationalState;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import io.github.tamawish.rwr.reset.ResetPhase;
import io.github.tamawish.rwr.reset.ResetStatus;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class BukkitRwrApiTest {
    @Test
    void snapshotsAreImmutableAndLookupIsCaseInsensitive() {
        ManagedWorldSettings world = world("resource", WorldOperationalState.MANAGED, true);
        PluginSettings settings = settings(world);
        BukkitRwrApi api = new BukkitRwrApi(
                () -> settings,
                id -> ResetStatus.idle(id, "resource_world"));

        assertThat(api.managedWorld("ReSoUrCe")).get().extracting(snapshot -> snapshot.id()).isEqualTo("resource");
        assertThat(api.resetStatus("RESOURCE")).get().satisfies(status -> {
            assertThat(status.phase()).isEqualTo(io.github.tamawish.rwr.api.model.ResetPhase.IDLE);
            assertThat(status.operationId()).isEmpty();
        });
        assertThatThrownBy(() -> api.managedWorlds().add(api.managedWorlds().getFirst()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsLatestSettingsOnEveryCall() {
        AtomicReference<PluginSettings> current = new AtomicReference<>(
                settings(world("resource", WorldOperationalState.MANAGED, true)));
        BukkitRwrApi api = new BukkitRwrApi(
                current::get,
                id -> new ResetStatus(id, "resource_world", ResetPhase.PRECHECK, "operation", "Checking"));

        assertThat(api.managedWorld("resource")).get().extracting(snapshot -> snapshot.resetCapable()).isEqualTo(true);
        current.set(settings(world("resource", WorldOperationalState.PROTECTED, false)));
        assertThat(api.managedWorld("RESOURCE")).get().satisfies(snapshot -> {
            assertThat(snapshot.resetCapable()).isFalse();
            assertThat(snapshot.state())
                    .isEqualTo(io.github.tamawish.rwr.api.model.ManagedWorldState.PROTECTED);
        });
        assertThat(api.resetStatus("resource")).get().satisfies(status -> {
            assertThat(status.isActive()).isTrue();
            assertThat(status.operationId()).contains("operation");
        });
    }

    @Test
    void rejectsInvalidIdsAndReturnsEmptyForUnknownIds() {
        BukkitRwrApi api = new BukkitRwrApi(
                () -> settings(world("resource", WorldOperationalState.MANAGED, true)),
                id -> ResetStatus.idle(id, "resource_world"));
        assertThat(api.managedWorld("missing")).isEmpty();
        assertThat(api.resetStatus("missing")).isEmpty();
        assertThatThrownBy(() -> api.managedWorld(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> api.resetStatus(null)).isInstanceOf(NullPointerException.class);
    }

    private static ManagedWorldSettings world(String id, WorldOperationalState state, boolean canReset) {
        ManagedWorldSettings world = new ManagedWorldSettings(
                id,
                "resource_world",
                "Resource World",
                canReset,
                true,
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.NOON, null, 1, 0),
                java.util.List.of(),
                new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                new EvacuationSettings(true, "world"),
                state);
        assertThat(world.canReset()).isEqualTo(canReset);
        return world;
    }

    private static PluginSettings settings(ManagedWorldSettings... worlds) {
        Map<String, ManagedWorldSettings> indexed = new LinkedHashMap<>();
        for (ManagedWorldSettings world : worlds) {
            indexed.put(world.id(), world);
        }
        return new PluginSettings(5, null, null, null, indexed, null);
    }
}
