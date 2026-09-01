package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tamawish.rwr.api.event.ResourceWorldResetWarningEvent;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.config.ScheduleSettings;
import io.github.tamawish.rwr.config.ScheduleType;
import io.github.tamawish.rwr.config.WorldOperationalState;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import io.github.tamawish.rwr.scheduler.WarningNotifier;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

class BukkitWarningEventPublisherTest {
    @Test
    void publishesMatchingEventAndPlayerWarning() {
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        WarningNotifier playerWarnings = mock(WarningNotifier.class);
        AtomicReference<ResourceWorldResetWarningEvent> published = new AtomicReference<>();
        ManagedWorldSettings world = managedWorld();
        ZonedDateTime resetAt = ZonedDateTime.of(2026, 8, 30, 12, 0, 0, 0, ZoneId.of("UTC"));
        when(server.getPluginManager()).thenReturn(plugins);
        org.mockito.Mockito.doAnswer(invocation -> {
                    published.set(invocation.getArgument(0));
                    return null;
                })
                .when(plugins)
                .callEvent(any());

        new BukkitWarningEventPublisher(server, playerWarnings).warn(world, 10, resetAt);

        ResourceWorldResetWarningEvent event = published.get();
        assertThat(event.getWorldId()).isEqualTo(world.id());
        assertThat(event.getWorldName()).isEqualTo(world.multiverseWorld());
        assertThat(event.getMinutesRemaining()).isEqualTo(10);
        assertThat(event.getScheduledResetAt()).isEqualTo(resetAt.toInstant());
        verify(playerWarnings).warn(world, 10, resetAt);
    }

    @Test
    void listenerFailureDoesNotSuppressPlayerWarning() {
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        WarningNotifier playerWarnings = mock(WarningNotifier.class);
        ManagedWorldSettings world = managedWorld();
        ZonedDateTime resetAt = ZonedDateTime.of(2026, 8, 30, 12, 0, 0, 0, ZoneId.of("UTC"));
        when(server.getPluginManager()).thenReturn(plugins);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        doThrow(new IllegalStateException("listener failed")).when(plugins).callEvent(any());

        new BukkitWarningEventPublisher(server, playerWarnings).warn(world, 10, resetAt);

        verify(playerWarnings).warn(world, 10, resetAt);
    }

    private static ManagedWorldSettings managedWorld() {
        return new ManagedWorldSettings(
                "resource",
                "resource_world",
                "Resource World",
                true,
                true,
                new ScheduleSettings(ScheduleType.DAILY, LocalTime.NOON, null, 1, 0),
                List.of(10),
                new RegenerationSettings(SeedPolicy.SAME, null, true, true, true),
                new EvacuationSettings(true, "world"),
                WorldOperationalState.MANAGED);
    }
}
