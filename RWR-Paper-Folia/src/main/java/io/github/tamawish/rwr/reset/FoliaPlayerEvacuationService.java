package io.github.tamawish.rwr.reset;

import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.world.BukkitLocations;
import io.github.tamawish.rwr.world.SafeLocation;
import io.github.tamawish.rwr.world.WorldProvider;
import io.github.tamawish.rwr.worlds.WorldsKeys;
import io.papermc.paper.ServerBuildInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;

/**
 * Paper/Folia-aware player evacuation.
 *
 * <p>Paper runs the global-region scheduler on its primary server thread. Waiting there for a
 * {@link Player#teleportAsync(Location)} future deadlocks because completion of the teleport also
 * requires that thread. Paper therefore uses the synchronous teleport API. On Folia the global
 * region must also remain responsive while entity-region teleports complete, so Folia evacuation is
 * exposed as a completion stage and never waits on a scheduler thread.
 */
public final class FoliaPlayerEvacuationService implements PlayerEvacuationService {
  private static final long TELEPORT_TIMEOUT_SECONDS = 30L;
  private static final boolean FOLIA = isFolia();

  private final Plugin plugin;
  private final Server server;
  private final WorldProvider gateway;
  private final WorldsKeys keys;

  /**
   * Creates a region-aware player evacuation service.
   *
   * @param plugin owning plugin
   * @param gateway world provider used to resolve the fallback destination
   * @param keys Worlds key resolver
   */
  public FoliaPlayerEvacuationService(Plugin plugin, WorldProvider gateway, WorldsKeys keys) {
    this.plugin = plugin;
    this.server = plugin.getServer();
    this.gateway = gateway;
    this.keys = keys;
  }

  @Override
  public EvacuationResult evacuate(String sourceWorld, EvacuationSettings settings) {
    Preparation preparation = prepare(sourceWorld, settings);
    if (preparation instanceof Complete complete) {
      return complete.result();
    }
    Ready ready = (Ready) preparation;
    if (FOLIA) {
      return new EvacuationResult.Failed(
          ResetFailureType.EVACUATION_FAILED,
          ready.players().size(),
          "Folia evacuation must be invoked asynchronously.");
    }
    return evacuateOnPaper(ready.source(), ready.players(), ready.target());
  }

  @Override
  public CompletionStage<EvacuationResult> evacuateAsync(
      String sourceWorld, EvacuationSettings settings) {
    if (FOLIA) {
      return evacuateStagedOnFolia(sourceWorld, settings);
    }
    Preparation preparation;
    try {
      preparation = prepare(sourceWorld, settings);
    } catch (RuntimeException exception) {
      return CompletableFuture.failedFuture(exception);
    }
    if (preparation instanceof Complete complete) {
      return CompletableFuture.completedFuture(complete.result());
    }
    Ready ready = (Ready) preparation;
    return CompletableFuture.completedFuture(
        evacuateOnPaper(ready.source(), ready.players(), ready.target()));
  }

  private CompletionStage<EvacuationResult> evacuateStagedOnFolia(
      String sourceWorld, EvacuationSettings settings) {
    World source = resolveLoaded(sourceWorld);
    if (source == null) {
      return CompletableFuture.completedFuture(
          new EvacuationResult.Failed(
              ResetFailureType.WORLD_NOT_LOADED,
              0,
              "The source world is no longer loaded in Bukkit."));
    }
    CompletableFuture<EvacuationResult> result = new CompletableFuture<>();
    runOnWorldRegion(
        source,
        result,
        () -> {
          List<Player> stagedPlayers = List.copyOf(source.getPlayers());
          if (stagedPlayers.isEmpty()) {
            result.complete(new EvacuationResult.Success(0));
            return;
          }
          if (!settings.enabled()) {
            result.complete(
                new EvacuationResult.Failed(
                    ResetFailureType.EVACUATION_DISABLED,
                    stagedPlayers.size(),
                    "Evacuation is disabled while the world contains players."));
            return;
          }
          gateway
              .resolveSafeDestinationAsync(settings.destination())
              .whenComplete(
                  (destination, error) -> {
                    if (error != null) {
                      result.completeExceptionally(error);
                      return;
                    }
                    if (destination instanceof DestinationResult.Unavailable unavailable) {
                      result.complete(
                          new EvacuationResult.Failed(
                              ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
                              stagedPlayers.size(),
                              unavailable.reason() + ": " + unavailable.message()));
                      return;
                    }
                    SafeLocation safe = ((DestinationResult.Available) destination).location();
                    Location target = resolveTargetLocation(safe, settings.destination());
                    if (target == null) {
                      result.complete(
                          new EvacuationResult.Failed(
                              ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
                              stagedPlayers.size(),
                              "Evacuation destination world is not loaded."));
                      return;
                    }
                    runOnWorldRegion(
                        source,
                        result,
                        () ->
                            evacuateOnFolia(source, List.copyOf(source.getPlayers()), target)
                                .whenComplete(
                                    (evacuation, evacuationError) -> {
                                      if (evacuationError == null) {
                                        result.complete(evacuation);
                                      } else {
                                        result.completeExceptionally(evacuationError);
                                      }
                                    }));
                  });
        });
    return result;
  }

  private Location resolveTargetLocation(SafeLocation safe, String destination) {
    Location target = BukkitLocations.toBukkit(safe, server);
    if (target != null) {
      return target;
    }
    World byKey = resolveLoaded(destination);
    return byKey != null ? byKey.getSpawnLocation() : null;
  }

  private Preparation prepare(String sourceWorld, EvacuationSettings settings) {
    World source = resolveLoaded(sourceWorld);
    if (source == null) {
      return new Complete(
          new EvacuationResult.Failed(
              ResetFailureType.WORLD_NOT_LOADED,
              0,
              "The source world is no longer loaded in Bukkit."));
    }
    List<Player> players = List.copyOf(source.getPlayers());
    if (players.isEmpty()) {
      return new Complete(new EvacuationResult.Success(0));
    }
    if (!settings.enabled()) {
      return new Complete(
          new EvacuationResult.Failed(
              ResetFailureType.EVACUATION_DISABLED,
              players.size(),
              "Evacuation is disabled while the world contains players."));
    }

    DestinationResult destination = gateway.resolveSafeDestination(settings.destination());
    if (destination instanceof DestinationResult.Unavailable unavailable) {
      return new Complete(
          new EvacuationResult.Failed(
              ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
              players.size(),
              unavailable.reason() + ": " + unavailable.message()));
    }
    SafeLocation safe = ((DestinationResult.Available) destination).location();
    Location target = resolveTargetLocation(safe, settings.destination());
    if (target == null) {
      return new Complete(
          new EvacuationResult.Failed(
              ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
              players.size(),
              "Evacuation destination world is not loaded."));
    }
    return new Ready(source, players, target);
  }

  private EvacuationResult evacuateOnPaper(World source, List<Player> players, Location target) {
    int failedTeleports = 0;
    for (Player player : players) {
      try {
        if (!player.teleport(target.clone(), TeleportCause.PLUGIN)) {
          failedTeleports++;
        } else {
          clearFallDistance(player);
        }
      } catch (RuntimeException exception) {
        server.getLogger().log(Level.WARNING, "Teleport failed for " + player.getName(), exception);
        failedTeleports++;
      }
    }

    int remaining = source.getPlayers().size();
    if (failedTeleports > 0 || remaining > 0) {
      return new EvacuationResult.Failed(
          ResetFailureType.EVACUATION_FAILED,
          remaining,
          failedTeleports + " teleport(s) failed and " + remaining + " player(s) remain.");
    }
    return new EvacuationResult.Success(players.size());
  }

  private CompletionStage<EvacuationResult> evacuateOnFolia(
      World source, List<Player> players, Location target) {
    List<CompletableFuture<Boolean>> futures = new ArrayList<>(players.size());
    for (Player player : players) {
      Location clone = target.clone();
      try {
        futures.add(
            player
                .teleportAsync(clone, TeleportCause.PLUGIN)
                .thenCompose(
                    teleported ->
                        Boolean.TRUE.equals(teleported)
                            ? clearFallDistanceAsync(player)
                            : CompletableFuture.completedFuture(false))
                .exceptionally(
                    error -> {
                      server
                          .getLogger()
                          .log(Level.WARNING, "Teleport failed for " + player.getName(), error);
                      return false;
                    }));
      } catch (RuntimeException exception) {
        server
            .getLogger()
            .log(Level.WARNING, "Failed to start teleport for " + player.getName(), exception);
        futures.add(CompletableFuture.completedFuture(false));
      }
    }

    CompletableFuture<EvacuationResult> result = new CompletableFuture<>();
    CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
        .orTimeout(TELEPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .whenComplete(
            (ignored, error) ->
                completeOnWorldRegion(
                    source,
                    result,
                    () -> {
                      int remaining = source.getPlayers().size();
                      if (error != null) {
                        result.complete(
                            new EvacuationResult.Failed(
                                ResetFailureType.EVACUATION_FAILED,
                                remaining,
                                "Evacuation teleports timed out after "
                                    + TELEPORT_TIMEOUT_SECONDS
                                    + "s."));
                        return;
                      }

                      long failedTeleports =
                          futures.stream()
                              .map(future -> future.getNow(false))
                              .filter(success -> !success)
                              .count();
                      if (failedTeleports > 0 || remaining > 0) {
                        result.complete(
                            new EvacuationResult.Failed(
                                ResetFailureType.EVACUATION_FAILED,
                                remaining,
                                failedTeleports
                                    + " teleport(s) failed and "
                                    + remaining
                                    + " player(s) remain."));
                        return;
                      }
                      result.complete(new EvacuationResult.Success(players.size()));
                    }));
    return result.orTimeout(TELEPORT_TIMEOUT_SECONDS + 5L, TimeUnit.SECONDS);
  }

  @Override
  public OptionalInt remainingPlayers(String sourceWorld) {
    World world = resolveLoaded(sourceWorld);
    return world == null ? OptionalInt.empty() : OptionalInt.of(world.getPlayers().size());
  }

  @Override
  public CompletionStage<OptionalInt> remainingPlayersAsync(String sourceWorld) {
    World world = resolveLoaded(sourceWorld);
    if (world == null) {
      return CompletableFuture.completedFuture(OptionalInt.empty());
    }
    CompletableFuture<OptionalInt> result = new CompletableFuture<>();
    runOnWorldRegion(
        world, result, () -> result.complete(OptionalInt.of(world.getPlayers().size())));
    return result;
  }

  private World resolveLoaded(String configured) {
    return keys.getWorld(configured).orElseGet(() -> server.getWorld(configured));
  }

  private void completeOnWorldRegion(
      World world, CompletableFuture<EvacuationResult> result, Runnable completion) {
    runOnWorldRegion(world, result, completion);
  }

  private <T> void runOnWorldRegion(World world, CompletableFuture<T> result, Runnable task) {
    try {
      server.getRegionScheduler().run(plugin, world, 0, 0, ignored -> task.run());
    } catch (RuntimeException error) {
      result.completeExceptionally(error);
    }
  }

  private void clearFallDistance(Player player) {
    player.setFallDistance(0.0F);
  }

  private CompletableFuture<Boolean> clearFallDistanceAsync(Player player) {
    CompletableFuture<Boolean> protectedTeleport = new CompletableFuture<>();
    player
        .getScheduler()
        .run(
            plugin,
            ignored -> {
              clearFallDistance(player);
              protectedTeleport.complete(true);
            },
            () -> protectedTeleport.complete(false));
    return protectedTeleport;
  }

  private static boolean isFolia() {
    try {
      return ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));
    } catch (RuntimeException | LinkageError ignored) {
      return false;
    }
  }

  private sealed interface Preparation permits Complete, Ready {}

  private record Complete(EvacuationResult result) implements Preparation {}

  private record Ready(World source, List<Player> players, Location target)
      implements Preparation {}
}
