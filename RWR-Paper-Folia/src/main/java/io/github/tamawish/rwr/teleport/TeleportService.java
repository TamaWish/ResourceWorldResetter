package io.github.tamawish.rwr.teleport;

import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
import io.github.tamawish.rwr.world.BukkitLocations;
import io.github.tamawish.rwr.world.SafeLocation;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
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

    public TeleportService(
            Supplier<TeleportSettings> settings,
            WorldProvider gateway,
            ResetAccessPolicy resetAccess) {
        this(settings, gateway, resetAccess, name -> name);
    }

    public TeleportService(
            Supplier<TeleportSettings> settings,
            WorldProvider gateway,
            ResetAccessPolicy resetAccess,
            Function<String, String> displayNames) {
        this.settings = settings;
        this.gateway = gateway;
        this.resetAccess = resetAccess;
        this.catalog = new TeleportDestinationCatalog(gateway, resetAccess, displayNames);
    }

    public TeleportPage page(PermissionChecker permissions, int requestedPage) {
        return catalog.page(settings.get(), permissions, requestedPage);
    }

    public CompletionStage<TeleportAttempt> teleport(Player player, String requestedWorld) {
        Optional<TeleportDestinationView> current =
                catalog.destination(settings.get(), player::hasPermission, requestedWorld);
        if (current.isEmpty()) {
            return completed(TeleportAttempt.failure("That destination is disabled or no longer configured."));
        }
        TeleportDestinationView destination = current.get();
        switch (destination.state()) {
            case LOCKED -> {
                return completed(TeleportAttempt.failure("You do not have permission for that destination."));
            }
            case UNAVAILABLE -> {
                return completed(TeleportAttempt.failure("That Worlds world is not currently loaded."));
            }
            case RESETTING -> {
                return completed(TeleportAttempt.failure("That world is currently resetting."));
            }
            case AVAILABLE -> {
                // Continue after all policy checks pass.
            }
        }

        DestinationResult resolved = gateway.resolveSafeDestination(destination.worldName());
        if (resolved instanceof DestinationResult.Unavailable unavailable) {
            return completed(TeleportAttempt.failure("Safe destination unavailable: " + unavailable.message()));
        }
        if (resetAccess.blocksIncomingRwrTeleport(destination.worldName())) {
            return completed(TeleportAttempt.failure("That world started resetting; teleport cancelled."));
        }
        SafeLocation safe = ((DestinationResult.Available) resolved).location();
        Location target = BukkitLocations.toBukkit(safe, player.getServer());
        if (target == null) {
            return completed(TeleportAttempt.failure("The destination world is not loaded."));
        }
        try {
            return player.teleportAsync(target, TeleportCause.PLUGIN)
                    .orTimeout(TELEPORT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handle((ok, error) -> {
                        if (error != null) {
                            Throwable cause = error.getCause() == null ? error : error.getCause();
                            return TeleportAttempt.failure("Teleport failed: " + cause.getMessage());
                        }
                        if (!Boolean.TRUE.equals(ok)) {
                            return TeleportAttempt.failure("The server rejected the teleport.");
                        }
                        return TeleportAttempt.success("Teleported to " + destination.displayName() + '.');
                    });
        } catch (RuntimeException exception) {
            return completed(TeleportAttempt.failure("Teleport failed: " + exception.getMessage()));
        }
    }

    private static CompletionStage<TeleportAttempt> completed(TeleportAttempt attempt) {
        return CompletableFuture.completedFuture(attempt);
    }
}
