package com.lozaine.resourceworldresetter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorldDisplayNamesTest {
    @Test
    void managedDisplayNameIsPresentationOnlyAndSharedByMultiverseName() {
        ManagedWorldSettings world = new ManagedWorldSettings(
                "rainforest-id",
                "rainforest",
                "Rainforester",
                true,
                true,
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.of(3, 0), null, 0, 0),
                List.of(30, 10, 5, 1),
                new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                new EvacuationSettings(true, "world"),
                WorldOperationalState.MANAGED);
        Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>();
        worlds.put(world.id(), world);
        PluginSettings settings = new PluginSettings(
                5,
                ZoneId.of("UTC"),
                "world",
                new ResetPolicySettings(2, 30, true),
                worlds,
                new TeleportSettings(true, false, true, Map.of()));

        assertThat(WorldDisplayNames.resolve(settings, "RAINFOREST")).isEqualTo("Rainforester");
        assertThat(WorldDisplayNames.resolveId(settings, "rainforest-id")).isEqualTo("Rainforester");
        assertThat(WorldDisplayNames.resolve(settings, "unmanaged")).isEqualTo("unmanaged");
        assertThat(world.id()).isEqualTo("rainforest-id");
        assertThat(world.multiverseWorld()).isEqualTo("rainforest");
    }
}
