package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.EvacuationProvider;
import io.github.tamawish.rwr.config.EvacuationDestination;
import io.github.tamawish.rwr.config.EvacuationDestinationType;
import io.github.tamawish.rwr.config.PluginSettings;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Runtime-only destination discovery and the sole owner of the proxy messaging channel. */
public final class DestinationCatalog implements PluginMessageListener, AutoCloseable {
  public static final String CHANNEL = "BungeeCord";
  private final Plugin plugin;
  private final java.util.function.Consumer<Runnable> timeout;
  private final Map<UUID, CompletableFuture<Boolean>> pending = new HashMap<>();
  private volatile List<String> servers = List.of();
  private volatile boolean verified;
  private boolean closed;

  public DestinationCatalog(Plugin plugin) {
    this(plugin, task -> CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(task));
  }

  DestinationCatalog(Plugin plugin, java.util.function.Consumer<Runnable> timeout) {
    this.plugin = plugin;
    this.timeout = timeout;
    plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
    plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
  }

  /** Call on the player's owning scheduler. Concurrent selectors share an in-flight request. */
  public synchronized CompletableFuture<Boolean> discover(Player player) {
    if (closed || !player.isOnline()) return CompletableFuture.completedFuture(false);
    UUID id = player.getUniqueId();
    CompletableFuture<Boolean> existing = pending.get(id);
    if (existing != null) return existing;
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    pending.put(id, result);
    timeout.accept(() -> expire(id, result));
    try {
      player.sendPluginMessage(plugin, CHANNEL, requestMessage());
    } catch (RuntimeException error) {
      expire(id, result);
    }
    return result;
  }

  private synchronized void expire(UUID id, CompletableFuture<Boolean> request) {
    if (pending.remove(id, request)) request.complete(false);
  }

  @Override
  public synchronized void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
    if (closed || !CHANNEL.equals(channel)) return;
    CompletableFuture<Boolean> request = pending.get(player.getUniqueId());
    if (request == null) return;
    Optional<List<String>> parsed = parseResponse(bytes);
    if (parsed.isEmpty()) return;
    pending.remove(player.getUniqueId());
    servers = parsed.get();
    verified = true;
    request.complete(true);
  }

  public static byte[] requestMessage() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      new DataOutputStream(bytes).writeUTF("GetServers");
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  public static Optional<List<String>> parseResponse(byte[] bytes) {
    if (bytes == null || bytes.length > 32766) return Optional.empty();
    try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
      if (!"GetServers".equals(input.readUTF())) return Optional.empty();
      String names = input.readUTF();
      if (input.available() != 0) return Optional.empty();
      Set<String> result = new LinkedHashSet<>();
      for (String value : names.split(",", -1)) {
        String name = value.trim();
        if (name.isEmpty()) continue;
        if (name.length() > 256 || name.chars().anyMatch(Character::isISOControl))
          return Optional.empty();
        result.add(name);
      }
      return Optional.of(List.copyOf(result));
    } catch (IOException error) {
      return Optional.empty();
    }
  }

  public record Entry(String name, boolean available, boolean verified) {}

  public List<Entry> entries(
      PluginSettings settings, EvacuationDestinationType type, EvacuationDestination current) {
    Map<String, Integer> live = new LinkedHashMap<>();
    if (type == EvacuationDestinationType.PROXY_SERVER) {
      servers.forEach(name -> live.put(name, 1));
    } else {
      plugin.getServer().getServicesManager().getRegistrations(EvacuationProvider.class).stream()
          .filter(registration -> registration.getPlugin().isEnabled())
          .forEach(
              registration -> {
                String name = registration.getProvider().destinationId();
                if (name != null && !name.isBlank()) live.merge(name, 1, Integer::sum);
              });
    }
    Set<String> names = new LinkedHashSet<>();
    addSaved(names, type, current);
    names.addAll(live.keySet());
    if (type == EvacuationDestinationType.PROXY_SERVER) names.addAll(settings.proxyServers());
    addSaved(names, type, settings.defaultEvacuation().typedDestination());
    settings
        .worlds()
        .values()
        .forEach(world -> addSaved(names, type, world.evacuation().typedDestination()));
    return names.stream()
        .map(
            name ->
                new Entry(
                    name,
                    live.getOrDefault(name, 0) == 1,
                    type != EvacuationDestinationType.PROXY_SERVER || verified))
        .toList();
  }

  private static void addSaved(
      Set<String> names, EvacuationDestinationType type, EvacuationDestination destination) {
    if (destination.type() == type && !destination.target().isBlank())
      names.add(destination.target());
  }

  public static void connect(Plugin plugin, Player player, String server) {
    player.sendPluginMessage(plugin, CHANNEL, DestinationEvacuationService.connectMessage(server));
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    var requests = List.copyOf(pending.values());
    pending.clear();
    servers = List.of();
    verified = false;
    requests.forEach(request -> request.complete(false));
    plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL, this);
    plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
  }
}
