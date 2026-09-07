package io.github.tamawish.rwr.teleport;

import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
import io.github.tamawish.rwr.reset.ResetAccessPolicy.TeleportPermit;
import io.github.tamawish.rwr.world.BukkitLocations;
import io.github.tamawish.rwr.world.SafeLocation;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/** Revalidates every click and completes without blocking the player/server tick thread. */
public final class TeleportService {
  private static final long TELEPORT_TIMEOUT_SECONDS = 15L;

  private final Supplier<TeleportSettings> settings;
  private final WorldProvider gateway;
  private final ResetAccessPolicy resetAccess;
  private final TeleportDestinationCatalog catalog;
  private final BiFunction<String, Object[], String> messages;

  /**
   * Creates a teleport service with provider-aware names and localized outcomes.
   *
   * @param settings source of active teleport settings
   * @param gateway world provider
   * @param resetAccess active reset access policy
   * @param displayNames resolver for player-facing world names
   * @param messages locale resolver receiving a key and alternating placeholder values
   */
  public TeleportService(
      Supplier<TeleportSettings> settings,
      WorldProvider gateway,
      ResetAccessPolicy resetAccess,
      Function<String, String> displayNames,
      BiFunction<String, Object[], String> messages) {
    this.settings = settings;
    this.gateway = gateway;
    this.resetAccess = resetAccess;
    this.catalog = new TeleportDestinationCatalog(gateway, resetAccess, displayNames);
    this.messages = messages;
  }

  /**
   * Returns a bounded page of currently visible destinations.
   *
   * @param permissions permission policy for the requesting player
   * @param requestedPage requested zero-based page index
   * @return destination page
   */
  public TeleportPage page(PermissionChecker permissions, int requestedPage) {
    return catalog.page(settings.get(), permissions, requestedPage);
  }

  /**
   * Revalidates and asynchronously teleports a player to a safe destination.
   *
   * @param player player requesting teleportation
   * @param requestedWorld provider-specific world name
   * @return a stage completed with the teleport outcome
   */
  public CompletionStage<TeleportAttempt> teleport(Player player, String requestedWorld) {
    Optional<TeleportDestinationView> current =
        catalog.destination(settings.get(), player::hasPermission, requestedWorld);
    if (current.isEmpty()) {
      return completed(TeleportAttempt.failure(message("teleport.not-configured")));
    }
    TeleportDestinationView destination = current.get();
    switch (destination.state()) {
      case LOCKED -> {
        return completed(TeleportAttempt.failure(message("teleport.no-permission")));
      }
      case UNAVAILABLE -> {
        return completed(TeleportAttempt.failure(message("teleport.unavailable")));
      }
      case RESETTING -> {
        return completed(TeleportAttempt.failure(message("teleport.resetting")));
      }
      case AVAILABLE -> {
        // Continue after all policy checks pass.
      }
      default -> throw new AssertionError("Unhandled destination state: " + destination.state());
    }

    Optional<TeleportPermit> admitted =
        resetAccess.tryAcquireIncomingRwrTeleport(destination.worldName());
    if (admitted.isEmpty()) {
      return completed(TeleportAttempt.failure(message("teleport.reset-started")));
    }
    TeleportPermit permit = admitted.get();
    try {
      DestinationResult resolved = gateway.resolveSafeDestination(destination.worldName());
      if (resolved instanceof DestinationResult.Unavailable) {
        permit.close();
        return completed(TeleportAttempt.failure(message("teleport.safe-unavailable")));
      }
      SafeLocation safe = ((DestinationResult.Available) resolved).location();
      Location target = BukkitLocations.toBukkit(safe, player.getServer());
      if (target == null) {
        permit.close();
        return completed(TeleportAttempt.failure(message("teleport.unavailable")));
      }
      return player
          .teleportAsync(target, TeleportCause.PLUGIN)
          .orTimeout(TELEPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .handle(
              (ok, error) -> {
                if (error != null) {
                  return TeleportAttempt.failure(message("teleport.failed"));
                }
                if (!Boolean.TRUE.equals(ok)) {
                  return TeleportAttempt.failure(message("teleport.rejected"));
                }
                return TeleportAttempt.success(
                    message("teleport.success", "world", destination.displayName()));
              })
          .whenComplete((attempt, error) -> permit.close());
    } catch (RuntimeException exception) {
      permit.close();
      return completed(TeleportAttempt.failure(message("teleport.failed")));
    }
  }

  private String message(String key, Object... placeholders) {
    return messages.apply(key, placeholders);
  }

  private static CompletionStage<TeleportAttempt> completed(TeleportAttempt attempt) {
    return CompletableFuture.completedFuture(attempt);
  }
}
