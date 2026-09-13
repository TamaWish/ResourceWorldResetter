package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.bukkitapi.DestinationEvacuationService;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Platform scheduling for typed evacuation destinations. */
public final class TypedPlayerEvacuationService extends DestinationEvacuationService {
  private final WorldProvider gateway;

  public TypedPlayerEvacuationService(Plugin plugin, WorldProvider gateway) {
    super(plugin);
    this.gateway = gateway;
  }

  @Override
  protected World resolveWorld(String name) {
    World direct = plugin.getServer().getWorld(name);
    if (direct != null) return direct;
    return plugin.getServer().getWorlds().stream()
        .filter(
            world ->
                world.getKey().toString().equals(name) || gateway.sameWorld(name, world.getName()))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected String defaultWorldName() {
    return gateway.defaultWorldName();
  }

  @Override
  protected CompletionStage<Location> localDestination(String name) {
    World world = resolveWorld(name);
    if (world == null) return CompletableFuture.completedFuture(null);
    CompletableFuture<Location> result = new CompletableFuture<>();
    onWorld(
        world,
        () -> {
          try {
            if (gateway.registeredWorldNames().stream()
                .anyMatch(value -> gateway.sameWorld(value, name))) {
              gateway
                  .resolveSafeDestinationAsync(name)
                  .whenComplete(
                      (destination, error) -> {
                        if (error != null) result.completeExceptionally(error);
                        else if (destination instanceof DestinationResult.Available available) {
                          var safe = available.location();
                          World resolved = resolveWorld(safe.worldName());
                          if (resolved == null) {
                            result.complete(null);
                            return;
                          }
                          result.complete(
                              new Location(
                                  resolved,
                                  safe.x(),
                                  safe.y(),
                                  safe.z(),
                                  safe.yaw(),
                                  safe.pitch()));
                        } else result.complete(null);
                      });
            } else result.complete(world.getSpawnLocation());
          } catch (RuntimeException error) {
            result.completeExceptionally(error);
          }
        });
    return result;
  }

  @Override
  protected void onWorld(World world, Runnable task) {
    if (plugin.getServer().isPrimaryThread()) task.run();
    else plugin.getServer().getScheduler().runTask(plugin, task);
  }

  @Override
  protected void onPlayer(Player player, Runnable task, Runnable retired) {
    onWorld(null, task);
  }

  @Override
  protected CompletionStage<Boolean> teleport(Player player, Location target) {
    return CompletableFuture.completedFuture(
        player.teleport(target, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN));
  }
}
