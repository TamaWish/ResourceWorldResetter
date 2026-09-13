package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.tamawish.rwr.api.EvacuationProvider;
import io.github.tamawish.rwr.config.EvacuationDestination;
import io.github.tamawish.rwr.config.EvacuationDestinationType;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.reset.EvacuationResult;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DestinationEvacuationServiceTest {
  private final Plugin plugin = mock(Plugin.class);
  private final Server server = mock(Server.class);
  private final ServicesManager services = mock(ServicesManager.class);
  private final World source = mock(World.class);
  private final World hub = mock(World.class);
  private final Player player = mock(Player.class);
  private final AtomicReference<List<Player>> occupants = new AtomicReference<>();
  private final AtomicReference<List<RegisteredServiceProvider<EvacuationProvider>>> providers =
      new AtomicReference<>(List.of());
  private TestService service;

  @BeforeEach
  void setup() {
    when(plugin.getServer()).thenReturn(server);
    when(plugin.isEnabled()).thenReturn(true);
    when(server.getMessenger()).thenReturn(mock(Messenger.class));
    when(server.getServicesManager()).thenReturn(services);
    when(services.getRegistrations(EvacuationProvider.class))
        .thenAnswer(ignored -> providers.get());
    when(source.getName()).thenReturn("resource");
    when(source.getUID()).thenReturn(UUID.randomUUID());
    when(hub.getUID()).thenReturn(UUID.randomUUID());
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.isOnline()).thenReturn(true);
    when(player.getWorld()).thenReturn(source);
    occupants.set(List.of(player));
    when(source.getPlayers()).thenAnswer(ignored -> occupants.get());
    service = new TestService();
  }

  @Test
  void proxyPayloadUsesConnectAndExactServerName() throws Exception {
    DataInputStream input =
        new DataInputStream(
            new ByteArrayInputStream(DestinationEvacuationService.connectMessage("Hub-1")));
    assertThat(input.readUTF()).isEqualTo("Connect");
    assertThat(input.readUTF()).isEqualTo("Hub-1");
    assertThat(input.available()).isZero();
  }

  @Test
  void proxyRequestRunsOnPlayerSchedulerAndWaitsForDeparture() throws Exception {
    doAnswer(
            ignored -> {
              assertThat(service.playerContext.get()).isTrue();
              return null;
            })
        .when(player)
        .sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
    CompletableFuture<EvacuationResult> result = evacuate(EvacuationDestinationType.PROXY_SERVER);
    verify(player).sendPluginMessage(eq(plugin), eq("BungeeCord"), any(byte[].class));
    assertThat(result).isNotDone();
    occupants.set(List.of());
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Success.class);
  }

  @Test
  void remainingPlayersCauseTimeoutEvenAfterProxyRequest() throws Exception {
    assertThat(evacuate(EvacuationDestinationType.PROXY_SERVER).get(3, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    assertThat(occupants.get()).containsExactly(player);
  }

  @Test
  void localTransferWaitsForAsyncTeleportAndClearsFallDistanceOnPlayerScheduler() throws Exception {
    doAnswer(
            ignored -> {
              assertThat(service.playerContext.get()).isTrue();
              return null;
            })
        .when(player)
        .setFallDistance(0.0F);
    CompletableFuture<EvacuationResult> result = evacuate(EvacuationDestinationType.LOCAL_WORLD);
    assertThat(result).isNotDone();
    occupants.set(List.of());
    service.teleported.complete(true);
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Success.class);
    verify(player).setFallDistance(0.0F);
  }

  @Test
  void defaultWorldResolvesPreset() throws Exception {
    CompletableFuture<EvacuationResult> result = evacuate(EvacuationDestinationType.DEFAULT_WORLD);
    assertThat(service.resolvedTarget).isEqualTo("overworld");
    occupants.set(List.of());
    service.teleported.complete(true);
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Success.class);
  }

  @Test
  void rejectsSourceDestinationBeforeTeleporting() throws Exception {
    service.location = new Location(source, 0, 80, 0);
    assertThat(evacuate(EvacuationDestinationType.LOCAL_WORLD).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    assertThat(service.teleportCalls).isZero();
  }

  @Test
  void rejectsUnloadedDestinationBeforeTeleporting() throws Exception {
    service.location = null;
    assertThat(evacuate(EvacuationDestinationType.LOCAL_WORLD).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    assertThat(service.teleportCalls).isZero();
  }

  @Test
  void missingProviderAborts() throws Exception {
    assertThat(evacuate(EvacuationDestinationType.REGISTERED_PROVIDER).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
  }

  @Test
  void providerSuccessStillRequiresDeparture() throws Exception {
    register(CompletableFuture.completedFuture(true));
    CompletableFuture<EvacuationResult> result =
        evacuate(EvacuationDestinationType.REGISTERED_PROVIDER);
    assertThat(result).isNotDone();
    occupants.set(List.of());
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Success.class);
  }

  @Test
  void providerUnregisterAbortsWhileFutureIsPending() throws Exception {
    register(new CompletableFuture<>());
    CompletableFuture<EvacuationResult> result =
        evacuate(EvacuationDestinationType.REGISTERED_PROVIDER);
    providers.set(List.of());
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Failed.class);
  }

  @Test
  void providerExceptionAborts() throws Exception {
    register(CompletableFuture.failedFuture(new IllegalStateException("provider broke")));
    assertThat(evacuate(EvacuationDestinationType.REGISTERED_PROVIDER).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
  }

  @Test
  void stalledProviderTimesOutEvenIfWorldBecomesEmpty() throws Exception {
    register(new CompletableFuture<>());
    CompletableFuture<EvacuationResult> result =
        evacuate(EvacuationDestinationType.REGISTERED_PROVIDER);
    occupants.set(List.of());
    assertThat(result.get(3, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Failed.class);
  }

  @Test
  void disabledEvacuationRefusesOccupiedWorld() throws Exception {
    assertThat(
            service
                .evacuateAsync("resource", new EvacuationSettings(false, "hub"))
                .toCompletableFuture()
                .get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    verify(player, never()).sendPluginMessage(any(), anyString(), any());
  }

  @Test
  void disconnectedPlayerCountsAsDeparted() throws Exception {
    when(player.isOnline()).thenReturn(false);
    CompletableFuture<EvacuationResult> result = evacuate(EvacuationDestinationType.PROXY_SERVER);
    occupants.set(List.of());
    assertThat(result.get(2, TimeUnit.SECONDS)).isInstanceOf(EvacuationResult.Success.class);
    verify(player, never()).sendPluginMessage(any(), anyString(), any());
  }

  @Test
  void duplicateProviderIdsAbortWithoutInvokingProvider() throws Exception {
    EvacuationProvider provider = register(CompletableFuture.completedFuture(true));
    var registration = providers.get().getFirst();
    providers.set(List.of(registration, registration));
    assertThat(evacuate(EvacuationDestinationType.REGISTERED_PROVIDER).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    verify(provider, never()).evacuate(any());
  }

  @Test
  void synchronousProviderThrowAborts() throws Exception {
    EvacuationProvider provider = register(CompletableFuture.completedFuture(true));
    doThrow(new IllegalStateException("provider threw")).when(provider).evacuate(any());
    assertThat(evacuate(EvacuationDestinationType.REGISTERED_PROVIDER).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
  }

  @Test
  void mixedTransferSuccessAndFailureAborts() throws Exception {
    Player second = mock(Player.class);
    when(second.isOnline()).thenReturn(true);
    when(second.getWorld()).thenReturn(source);
    when(second.getUniqueId()).thenReturn(UUID.randomUUID());
    occupants.set(List.of(player, second));
    service.failedPlayer = second;
    service.teleported.complete(true);
    assertThat(evacuate(EvacuationDestinationType.LOCAL_WORLD).get(2, TimeUnit.SECONDS))
        .isInstanceOf(EvacuationResult.Failed.class);
    assertThat(service.teleportCalls).isEqualTo(2);
  }

  private CompletableFuture<EvacuationResult> evacuate(EvacuationDestinationType type) {
    return service
        .evacuateAsync(
            "resource", new EvacuationSettings(true, new EvacuationDestination(type, "hub"), 1))
        .toCompletableFuture();
  }

  private EvacuationProvider register(CompletionStage<Boolean> completion) {
    EvacuationProvider provider = mock(EvacuationProvider.class);
    when(provider.destinationId()).thenReturn("hub");
    when(provider.evacuate(any()))
        .thenAnswer(
            ignored -> {
              assertThat(service.playerContext.get()).isTrue();
              return completion;
            });
    providers.set(
        List.of(
            new RegisteredServiceProvider<>(
                EvacuationProvider.class, provider, ServicePriority.Normal, plugin)));
    return provider;
  }

  private final class TestService extends DestinationEvacuationService {
    private Location location = new Location(hub, 0, 80, 0);
    private final CompletableFuture<Boolean> teleported = new CompletableFuture<>();
    private final ThreadLocal<Boolean> playerContext = ThreadLocal.withInitial(() -> false);
    private int teleportCalls;
    private Player failedPlayer;
    private String resolvedTarget;

    TestService() {
      super(DestinationEvacuationServiceTest.this.plugin);
    }

    @Override
    protected World resolveWorld(String name) {
      return source;
    }

    @Override
    protected void onWorld(World world, Runnable task) {
      task.run();
    }

    @Override
    protected void onPlayer(Player player, Runnable task, Runnable retired) {
      playerContext.set(true);
      try {
        task.run();
      } finally {
        playerContext.set(false);
      }
    }

    @Override
    protected CompletionStage<Boolean> teleport(Player player, Location target) {
      assertThat(playerContext.get()).isTrue();
      teleportCalls++;
      return player == failedPlayer ? CompletableFuture.completedFuture(false) : teleported;
    }

    @Override
    protected CompletionStage<Location> localDestination(String name) {
      resolvedTarget = name;
      return CompletableFuture.completedFuture(location);
    }

    @Override
    protected String defaultWorldName() {
      return "overworld";
    }
  }
}
