package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.EvacuationProvider;
import io.github.tamawish.rwr.config.EvacuationDestinationType;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.reset.EvacuationDestinationHandler;
import io.github.tamawish.rwr.reset.EvacuationResult;
import io.github.tamawish.rwr.reset.PlayerEvacuationService;
import io.github.tamawish.rwr.reset.ResetFailureType;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Shared bounded transfer orchestration; all Bukkit access is dispatched by the platform. */
public abstract class DestinationEvacuationService implements PlayerEvacuationService {
  protected final Plugin plugin;

  protected DestinationEvacuationService(Plugin plugin) {
    this.plugin = plugin;
  }

  protected abstract World resolveWorld(String name);

  protected abstract void onWorld(World world, Runnable task);

  protected abstract void onPlayer(Player player, Runnable task, Runnable retired);

  protected abstract CompletionStage<Boolean> teleport(Player player, Location target);

  protected abstract CompletionStage<Location> localDestination(String name);

  protected abstract String defaultWorldName();

  @Override
  public EvacuationResult evacuate(String source, EvacuationSettings settings) {
    // Legacy synchronous callers cannot safely wait for proxy/provider/async teleport completion.
    return failed("Use asynchronous evacuation for typed destinations.");
  }

  @Override
  public CompletionStage<EvacuationResult> evacuateAsync(String name, EvacuationSettings settings) {
    World source = resolveWorld(name);
    if (source == null)
      return CompletableFuture.completedFuture(failed("Source world is unloaded."));
    CompletableFuture<EvacuationResult> result = new CompletableFuture<>();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(settings.timeoutSeconds());
    // A wall-clock bound also covers a provider or teleport that never completes.
    CompletableFuture.delayedExecutor(settings.timeoutSeconds(), TimeUnit.SECONDS)
        .execute(
            () ->
                dispatch(
                    source,
                    result,
                    () -> result.complete(failed("Evacuation transfer timed out."))));
    dispatch(
        source,
        result,
        () -> {
          List<Player> players = List.copyOf(source.getPlayers());
          if (players.isEmpty()) {
            result.complete(new EvacuationResult.Success(0));
            return;
          }
          if (!settings.enabled()) {
            result.complete(
                new EvacuationResult.Failed(
                    ResetFailureType.EVACUATION_DISABLED,
                    players.size(),
                    "Evacuation is disabled while players remain."));
            return;
          }
          EvacuationDestinationType type = settings.typedDestination().type();
          if (type == EvacuationDestinationType.LOCAL_WORLD
              || type == EvacuationDestinationType.DEFAULT_WORLD) {
            String target =
                type == EvacuationDestinationType.DEFAULT_WORLD
                    ? defaultWorldName()
                    : settings.destination();
            localDestination(target)
                .whenComplete(
                    (location, error) ->
                        dispatch(
                            source,
                            result,
                            () -> {
                              if (error != null
                                  || location == null
                                  || location.getWorld() == null
                                  || location.getWorld().getUID().equals(source.getUID())) {
                                result.complete(
                                    failed(
                                        "Local destination is unavailable or is the source world."));
                                return;
                              }
                              start(source, players, settings, location, null, deadline, result);
                            }));
          } else {
            RegisteredServiceProvider<EvacuationProvider> provider = null;
            if (type == EvacuationDestinationType.REGISTERED_PROVIDER) {
              List<RegisteredServiceProvider<EvacuationProvider>> matches =
                  registrations(settings.destination());
              if (matches.size() != 1) {
                result.complete(
                    failed(
                        "Provider destination is missing or duplicated: "
                            + settings.destination()));
                return;
              }
              provider = matches.getFirst();
            }
            start(source, players, settings, null, provider, deadline, result);
          }
        });
    return result;
  }

  private void start(
      World source,
      List<Player> players,
      EvacuationSettings settings,
      Location location,
      RegisteredServiceProvider<EvacuationProvider> provider,
      long deadline,
      CompletableFuture<EvacuationResult> result) {
    List<CompletableFuture<Boolean>> transfers =
        players.stream()
            .map(
                player -> {
                  CompletableFuture<Boolean> transfer = new CompletableFuture<>();
                  onPlayer(
                      player,
                      () -> {
                        if (result.isDone()) return;
                        try {
                          if (!player.isOnline()
                              || !player.getWorld().getUID().equals(source.getUID())) {
                            transfer.complete(true);
                            return;
                          }
                          EvacuationDestinationHandler handler =
                              handler(player, location, provider);
                          CompletionStage<Boolean> stage =
                              handler.evacuate(player.getUniqueId(), settings.typedDestination());
                          stage.whenComplete(
                              (success, error) -> {
                                if (error != null || !Boolean.TRUE.equals(success))
                                  transfer.complete(false);
                                else if (location == null) transfer.complete(true);
                                else
                                  onPlayer(
                                      player,
                                      () -> {
                                        player.setFallDistance(0.0F);
                                        transfer.complete(true);
                                      },
                                      () -> transfer.complete(true));
                              });
                        } catch (RuntimeException error) {
                          transfer.complete(false);
                        }
                      },
                      () -> transfer.complete(true));
                  return transfer;
                })
            .toList();
    poll(source, players.size(), settings, provider, transfers, deadline, result);
  }

  private EvacuationDestinationHandler handler(
      Player player, Location location, RegisteredServiceProvider<EvacuationProvider> provider) {
    return (playerId, destination) -> {
      if (location != null) return teleport(player, location.clone());
      if (provider != null) {
        return available(destination.target(), provider)
            ? provider.getProvider().evacuate(playerId)
            : CompletableFuture.completedFuture(false);
      }
      DestinationCatalog.connect(plugin, player, destination.target());
      // Request accepted locally; only departure polling can establish success.
      return CompletableFuture.completedFuture(true);
    };
  }

  private void poll(
      World source,
      int count,
      EvacuationSettings settings,
      RegisteredServiceProvider<EvacuationProvider> provider,
      List<CompletableFuture<Boolean>> transfers,
      long deadline,
      CompletableFuture<EvacuationResult> result) {
    dispatch(
        source,
        result,
        () -> {
          if (resolveWorld(source.getName()) == null) {
            result.complete(failed("Source world unloaded during evacuation."));
          } else if (provider != null && !available(settings.destination(), provider)) {
            result.complete(failed("Provider destination was unregistered during evacuation."));
          } else if (transfers.stream().anyMatch(stage -> stage.isDone() && !stage.getNow(false))) {
            result.complete(failed("One or more evacuation transfers failed."));
          } else if (transfers.stream().allMatch(CompletableFuture::isDone)
              && source.getPlayers().isEmpty()) {
            result.complete(new EvacuationResult.Success(count));
          } else if (System.nanoTime() >= deadline) {
            result.complete(failed("Players have not departed before the transfer timeout."));
          } else {
            CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS)
                .execute(
                    () -> poll(source, count, settings, provider, transfers, deadline, result));
          }
        });
  }

  private List<RegisteredServiceProvider<EvacuationProvider>> registrations(String id) {
    return plugin
        .getServer()
        .getServicesManager()
        .getRegistrations(EvacuationProvider.class)
        .stream()
        .filter(value -> value.getPlugin().isEnabled())
        .filter(value -> id.equals(value.getProvider().destinationId()))
        .toList();
  }

  private boolean available(String id, RegisteredServiceProvider<EvacuationProvider> provider) {
    List<RegisteredServiceProvider<EvacuationProvider>> matches = registrations(id);
    return matches.size() == 1 && matches.getFirst() == provider;
  }

  public static byte[] connectMessage(String server) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeUTF("Connect");
      output.writeUTF(server);
      return bytes.toByteArray();
    } catch (IOException impossible) {
      throw new IllegalArgumentException("Invalid proxy server name", impossible);
    }
  }

  private <T> void dispatch(World world, CompletableFuture<T> result, Runnable task) {
    if (result.isDone()) return;
    try {
      if (!plugin.isEnabled()) throw new IllegalStateException("RWR is disabled");
      onWorld(
          world,
          () -> {
            if (result.isDone()) return;
            try {
              task.run();
            } catch (RuntimeException error) {
              result.completeExceptionally(error);
            }
          });
    } catch (RuntimeException error) {
      result.completeExceptionally(error);
    }
  }

  private static EvacuationResult.Failed failed(String message) {
    return new EvacuationResult.Failed(ResetFailureType.EVACUATION_FAILED, 0, message);
  }

  @Override
  public OptionalInt remainingPlayers(String name) {
    throw new IllegalStateException("Use remainingPlayersAsync on the owning scheduler");
  }

  @Override
  public CompletionStage<OptionalInt> remainingPlayersAsync(String name) {
    World source = resolveWorld(name);
    if (source == null) return CompletableFuture.completedFuture(OptionalInt.empty());
    CompletableFuture<OptionalInt> result = new CompletableFuture<>();
    dispatch(source, result, () -> result.complete(OptionalInt.of(source.getPlayers().size())));
    return result;
  }
}
