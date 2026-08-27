package com.lozaine.resourceworldresetter.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.config.TeleportDestinationSettings;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import com.lozaine.resourceworldresetter.multiverse.RegenerationOutcome;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRequest;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TeleportDestinationCatalogTest {
    @Test
    void explicitOverridesRemainIndependentWhenDiscoveryIsDisabled() {
        FakeGateway gateway = gateway(world("resource", true), world("unconfigured", true));
        TeleportSettings settings = settings(
                false,
                false,
                true,
                Map.of("resource", destination(true, "Mining World", null)));

        List<TeleportDestinationView> result = catalog(gateway, Set.of()).destinations(settings, value -> false);

        assertThat(result).singleElement().satisfies(destination -> {
            assertThat(destination.worldName()).isEqualTo("resource");
            assertThat(destination.displayName()).isEqualTo("Mining World");
            assertThat(destination.explicitOverride()).isTrue();
            assertThat(destination.state()).isEqualTo(TeleportDestinationState.AVAILABLE);
        });
    }

    @Test
    void discoveryUsesDefaultsAndOmitsDisabledDestinations() {
        FakeGateway gateway = gateway(world("world", true), world("resource", true));
        TeleportSettings settings = settings(
                true,
                true,
                true,
                Map.of("resource", destination(false, "Hidden", null)));

        assertThat(catalog(gateway, Set.of()).destinations(settings, value -> false))
                .extracting(TeleportDestinationView::worldName)
                .containsExactly("world");
    }

    @Test
    void customAndWildcardPermissionsControlLockedState() {
        FakeGateway gateway = gateway(world("resource", true));
        TeleportSettings settings = settings(
                false,
                false,
                true,
                Map.of("resource", destination(true, "Resource", "rank.vip")));
        TeleportDestinationCatalog catalog = catalog(gateway, Set.of());

        assertThat(catalog.destinations(settings, value -> false))
                .singleElement()
                .extracting(TeleportDestinationView::state)
                .isEqualTo(TeleportDestinationState.LOCKED);
        assertThat(catalog.destinations(settings, "rank.vip"::equals))
                .singleElement()
                .extracting(TeleportDestinationView::state)
                .isEqualTo(TeleportDestinationState.AVAILABLE);
        assertThat(catalog.destinations(settings, TeleportDestinationCatalog.WILDCARD_PERMISSION::equals))
                .singleElement()
                .extracting(TeleportDestinationView::state)
                .isEqualTo(TeleportDestinationState.AVAILABLE);
    }

    @Test
    void showLockedFalseHidesOnlyLockedEntries() {
        FakeGateway gateway = gateway(world("locked", true), world("offline", false));
        TeleportSettings settings = settings(
                false,
                false,
                false,
                Map.of(
                        "locked", destination(true, "Locked", "rank.vip"),
                        "offline", destination(true, "Offline", null)));

        assertThat(catalog(gateway, Set.of()).destinations(settings, value -> false))
                .singleElement()
                .satisfies(destination -> {
                    assertThat(destination.worldName()).isEqualTo("offline");
                    assertThat(destination.state()).isEqualTo(TeleportDestinationState.UNAVAILABLE);
                });
    }

    @Test
    void unloadedAndResettingWorldsAreUnavailableWithoutImplicitLoading() {
        FakeGateway gateway = gateway(world("offline", false), world("resetting", true));
        TeleportSettings settings = settings(
                true,
                true,
                true,
                Map.of());

        List<TeleportDestinationView> result =
                catalog(gateway, Set.of("resetting")).destinations(settings, value -> false);

        assertThat(result).filteredOn(destination -> destination.worldName().equals("offline"))
                .singleElement()
                .extracting(TeleportDestinationView::state)
                .isEqualTo(TeleportDestinationState.UNAVAILABLE);
        assertThat(result).filteredOn(destination -> destination.worldName().equals("resetting"))
                .singleElement()
                .extracting(TeleportDestinationView::state)
                .isEqualTo(TeleportDestinationState.RESETTING);
        assertThat(gateway.loadCalls).isZero();
    }

    @Test
    void paginationUsesFortyFiveDestinationsAndClampsRequestedPage() {
        List<WorldSnapshot> worlds = new ArrayList<>();
        for (int index = 0; index < 46; index++) {
            worlds.add(world("world_" + String.format("%02d", index), true));
        }
        FakeGateway gateway = gateway(worlds.toArray(WorldSnapshot[]::new));
        TeleportSettings settings = settings(true, true, true, Map.of());
        TeleportDestinationCatalog catalog = catalog(gateway, Set.of());

        TeleportPage first = catalog.page(settings, value -> false, 0);
        TeleportPage last = catalog.page(settings, value -> false, 99);

        assertThat(first.destinations()).hasSize(45);
        assertThat(first.hasPrevious()).isFalse();
        assertThat(first.hasNext()).isTrue();
        assertThat(last.page()).isEqualTo(1);
        assertThat(last.destinations()).hasSize(1);
        assertThat(last.hasPrevious()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    private static TeleportDestinationCatalog catalog(FakeGateway gateway, Set<String> resetting) {
        return new TeleportDestinationCatalog(
                gateway,
                world -> resetting.contains(world),
                name -> name.equalsIgnoreCase("resource") ? "Mining World" : name);
    }

    private static TeleportSettings settings(
            boolean discovery,
            boolean defaultEnabled,
            boolean showLocked,
            Map<String, TeleportDestinationSettings> worlds) {
        return new TeleportSettings(discovery, defaultEnabled, showLocked, worlds);
    }

    private static TeleportDestinationSettings destination(
            boolean enabled, String displayName, String permission) {
        return new TeleportDestinationSettings(enabled, permission);
    }

    private static WorldSnapshot world(String name, boolean loaded) {
        return new WorldSnapshot(
                name,
                name,
                name,
                loaded,
                "NORMAL",
                1L,
                "",
                "",
                "NORMAL",
                true,
                true,
                name + " 0 64 0");
    }

    private static FakeGateway gateway(WorldSnapshot... worlds) {
        return new FakeGateway(List.of(worlds));
    }

    private static final class FakeGateway implements WorldProvider {
        private final List<WorldSnapshot> worlds;
        private int loadCalls;

        private FakeGateway(List<WorldSnapshot> worlds) {
            this.worlds = worlds;
        }

        @Override
        public List<WorldSnapshot> registeredWorlds() {
            return worlds;
        }

        @Override
        public List<WorldSnapshot> loadedWorlds() {
            loadCalls++;
            return worlds.stream().filter(WorldSnapshot::loaded).toList();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> registeredWorldNames() {
            Set<String> names = new HashSet<>();
            worlds.forEach(world -> names.add(world.name()));
            return Set.copyOf(names);
        }

        @Override
        public String defaultWorldName() {
            return worlds.getFirst().name();
        }
    }
}
