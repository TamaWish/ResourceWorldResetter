package io.github.tamawish.rwr.teleport;

import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
import io.github.tamawish.rwr.world.BukkitLocations;
import io.github.tamawish.rwr.world.SafeLocation;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/** Revalidates every click and performs one synchronous Bukkit teleport. */
public final class TeleportService {
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

    public TeleportAttempt teleport(Player player, String requestedWorld) {
        Optional<TeleportDestinationView> current =
                catalog.destination(settings.get(), player::hasPermission, requestedWorld);
        if (current.isEmpty()) {
            return TeleportAttempt.failure("That destination is disabled or no longer configured.");
        }
        TeleportDestinationView destination = current.get();
        switch (destination.state()) {
            case LOCKED -> {
                return TeleportAttempt.failure("You do not have permission for that destination.");
            }
            case UNAVAILABLE -> {
                return TeleportAttempt.failure("That Multiverse world is not currently loaded.");
            }
            case RESETTING -> {
                return TeleportAttempt.failure("That world is currently resetting.");
            }
            case AVAILABLE -> {
                // Continue after all policy checks pass.
            }
        }

        DestinationResult resolved = gateway.resolveSafeDestination(destination.worldName());
        if (resolved instanceof DestinationResult.Unavailable unavailable) {
            return TeleportAttempt.failure("Safe destination unavailable: " + unavailable.message());
        }
        if (resetAccess.blocksIncomingRwrTeleport(destination.worldName())) {
            return TeleportAttempt.failure("That world started resetting; teleport cancelled.");
        }
        SafeLocation safe = ((DestinationResult.Available) resolved).location();
        Location target = BukkitLocations.toBukkit(safe, player.getServer());
        if (target == null || !player.teleport(target, TeleportCause.PLUGIN)) {
            return TeleportAttempt.failure("The server rejected the teleport.");
        }
        return TeleportAttempt.success("Teleported to " + destination.displayName() + '.');
    }
}
