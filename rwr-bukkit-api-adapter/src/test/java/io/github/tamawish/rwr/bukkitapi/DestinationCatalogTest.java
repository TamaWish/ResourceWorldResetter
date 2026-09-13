package io.github.tamawish.rwr.bukkitapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.github.tamawish.rwr.api.EvacuationProvider;
import io.github.tamawish.rwr.config.*;
import java.io.*;
import java.util.*;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.*;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.*;

class DestinationCatalogTest {
  private final Plugin plugin = mock(Plugin.class);
  private final Server server = mock(Server.class);
  private final Messenger messenger = mock(Messenger.class);
  private final ServicesManager services = mock(ServicesManager.class);
  private final List<Runnable> timeouts = new ArrayList<>();
  private final PluginSettings settings = mock(PluginSettings.class);
  private DestinationCatalog catalog;

  @BeforeEach
  void setup() {
    when(plugin.getServer()).thenReturn(server);
    when(server.getMessenger()).thenReturn(messenger);
    when(server.getServicesManager()).thenReturn(services);
    when(settings.defaultEvacuation()).thenReturn(new EvacuationSettings(true, "world"));
    when(settings.worlds()).thenReturn(Map.of());
    when(settings.proxyServers()).thenReturn(List.of());
    catalog = new DestinationCatalog(plugin, timeouts::add);
  }

  @AfterEach
  void close() {
    catalog.close();
  }

  @Test
  void requestUsesOnlyGetServers() throws Exception {
    var input = new DataInputStream(new ByteArrayInputStream(DestinationCatalog.requestMessage()));
    assertThat(input.readUTF()).isEqualTo("GetServers");
    assertThat(input.available()).isZero();
    verify(messenger).registerOutgoingPluginChannel(plugin, "BungeeCord");
    verify(messenger).registerIncomingPluginChannel(plugin, "BungeeCord", catalog);
  }

  @Test
  void parsesWhitespaceDuplicatesEmptyAndExactCase() throws Exception {
    assertThat(
            DestinationCatalog.parseResponse(response(" Lobby, resource, Lobby, lobby, , "))
                .orElseThrow())
        .containsExactly("Lobby", "resource", "lobby");
    assertThat(DestinationCatalog.parseResponse(response("")).orElseThrow()).isEmpty();
  }

  @Test
  void rejectsMalformedTruncatedWrongSubchannelOversizedAndControlNames() throws Exception {
    assertThat(DestinationCatalog.parseResponse(new byte[] {0, 20, 1})).isEmpty();
    assertThat(DestinationCatalog.parseResponse(DestinationCatalog.requestMessage())).isEmpty();
    assertThat(
            DestinationCatalog.parseResponse(DestinationEvacuationService.connectMessage("lobby")))
        .isEmpty();
    assertThat(DestinationCatalog.parseResponse(new byte[32767])).isEmpty();
    assertThat(DestinationCatalog.parseResponse(response("bad\nname"))).isEmpty();
    assertThat(DestinationCatalog.parseResponse(response("x".repeat(257)))).isEmpty();
    byte[] valid = response("lobby");
    assertThat(DestinationCatalog.parseResponse(Arrays.copyOf(valid, valid.length + 1))).isEmpty();
  }

  @Test
  void mergesAllSavedTargetsAndPlacesCurrentFirstWithoutChangingSpelling() throws Exception {
    when(settings.proxyServers()).thenReturn(List.of("Lobby", "configured", "Lobby"));
    when(settings.defaultEvacuation()).thenReturn(evacuation("global"));
    var world = mock(ManagedWorldSettings.class);
    when(world.evacuation()).thenReturn(evacuation("manual"));
    when(settings.worlds()).thenReturn(Map.of("resource", world));
    Player player = player();
    catalog.discover(player);
    catalog.onPluginMessageReceived("BungeeCord", player, response("resource,Lobby"));
    var entries =
        catalog.entries(
            settings,
            EvacuationDestinationType.PROXY_SERVER,
            evacuation("manual").typedDestination());
    assertThat(entries)
        .extracting(DestinationCatalog.Entry::name)
        .containsExactly("manual", "resource", "Lobby", "configured", "global");
    assertThat(entries)
        .filteredOn(DestinationCatalog.Entry::available)
        .extracting(DestinationCatalog.Entry::name)
        .containsExactly("resource", "Lobby");
    assertThat(entries.getFirst().verified()).isTrue();
  }

  @Test
  void timeoutKeepsConfiguredEntriesUnverifiedAndIgnoresLateUnsolicitedResponses()
      throws Exception {
    when(settings.proxyServers()).thenReturn(List.of("lobby"));
    Player player = player();
    var request = catalog.discover(player);
    timeouts.getFirst().run();
    assertThat(request.join()).isFalse();
    catalog.onPluginMessageReceived("BungeeCord", player, response("late"));
    var entries =
        catalog.entries(
            settings,
            EvacuationDestinationType.PROXY_SERVER,
            evacuation("saved").typedDestination());
    assertThat(entries)
        .extracting(DestinationCatalog.Entry::name)
        .containsExactly("saved", "lobby");
    assertThat(entries).allMatch(entry -> !entry.available() && !entry.verified());
  }

  @Test
  void administratorsHaveSeparateRequestsAndOldTimeoutCannotExpireNewRequest() throws Exception {
    Player first = player();
    Player second = player();
    var a = catalog.discover(first);
    assertThat(catalog.discover(first)).isSameAs(a);
    var b = catalog.discover(second);
    catalog.onPluginMessageReceived("BungeeCord", first, response("lobby"));
    assertThat(a.join()).isTrue();
    assertThat(b).isNotDone();
    var next = catalog.discover(first);
    timeouts.getFirst().run();
    assertThat(next).isNotDone();
    catalog.onPluginMessageReceived("BungeeCord", first, response("resource"));
    assertThat(next.join()).isTrue();
    assertThat(b).isNotDone();
  }

  @Test
  void providersFollowRegistrationLifecycleAndDuplicatesAreDisabled() {
    var owner = mock(Plugin.class);
    when(owner.isEnabled()).thenReturn(true);
    var provider = mock(EvacuationProvider.class);
    when(provider.destinationId()).thenReturn("portal");
    var registration =
        new RegisteredServiceProvider<>(
            EvacuationProvider.class, provider, ServicePriority.Normal, owner);
    var target = new EvacuationDestination(EvacuationDestinationType.REGISTERED_PROVIDER, "portal");
    when(services.getRegistrations(EvacuationProvider.class)).thenReturn(List.of(registration));
    assertThat(catalog.entries(settings, target.type(), target).getFirst().available()).isTrue();
    when(services.getRegistrations(EvacuationProvider.class))
        .thenReturn(List.of(registration, registration));
    assertThat(catalog.entries(settings, target.type(), target).getFirst().available()).isFalse();
    when(services.getRegistrations(EvacuationProvider.class)).thenReturn(List.of(registration));
    when(owner.isEnabled()).thenReturn(false);
    assertThat(catalog.entries(settings, target.type(), target).getFirst().available()).isFalse();
    when(services.getRegistrations(EvacuationProvider.class)).thenReturn(List.of());
    assertThat(catalog.entries(settings, target.type(), target))
        .containsExactly(new DestinationCatalog.Entry("portal", false, true));
  }

  @Test
  void shutdownCompletesPendingAndUnregistersChannels() {
    Player player = player();
    var request = catalog.discover(player);
    catalog.close();
    assertThat(request.join()).isFalse();
    assertThat(catalog.discover(player).join()).isFalse();
    verify(messenger).unregisterIncomingPluginChannel(plugin, "BungeeCord", catalog);
    verify(messenger).unregisterOutgoingPluginChannel(plugin, "BungeeCord");
  }

  private static Player player() {
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.isOnline()).thenReturn(true);
    return player;
  }

  private static EvacuationSettings evacuation(String name) {
    return new EvacuationSettings(
        true, new EvacuationDestination(EvacuationDestinationType.PROXY_SERVER, name), 30);
  }

  private static byte[] response(String servers) throws Exception {
    var bytes = new ByteArrayOutputStream();
    var output = new DataOutputStream(bytes);
    output.writeUTF("GetServers");
    output.writeUTF(servers);
    return bytes.toByteArray();
  }
}
