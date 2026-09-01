package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.tamawish.rwr.api.event.ResourceWorldPreResetEvent;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.config.ScheduleSettings;
import io.github.tamawish.rwr.config.ScheduleType;
import io.github.tamawish.rwr.config.WorldOperationalState;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import java.time.LocalTime;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

class BukkitResetEventPublisherTest {
    @Test
    void listenerCancellationStopsTheReset() {
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        ManagedWorldSettings world = new ManagedWorldSettings(
                "resource",
                "resource_world",
                "Resource World",
                true,
                true,
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.NOON, null, 1, 0),
                List.of(),
                new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                new EvacuationSettings(true, "world"),
                WorldOperationalState.MANAGED);
        when(server.getPluginManager()).thenReturn(plugins);
        doAnswer(invocation -> {
                    if (invocation.getArgument(0) instanceof ResourceWorldPreResetEvent event) {
                        event.setCancelled(true);
                    }
                    return null;
                })
                .when(plugins)
                .callEvent(any());

        assertThat(new BukkitResetEventPublisher(server).beforeReset(world, "operation")).isFalse();
    }
}
