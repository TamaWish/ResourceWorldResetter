package com.lozaine.resourceworldresetter.reset;

import com.lozaine.resourceworldresetter.config.EvacuationSettings;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.world.BukkitLocations;
import com.lozaine.resourceworldresetter.world.SafeLocation;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import java.util.List;
import java.util.OptionalInt;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public final class BukkitPlayerEvacuationService implements PlayerEvacuationService {
    private final Server server;
    private final WorldProvider gateway;

    public BukkitPlayerEvacuationService(Server server, WorldProvider gateway) {
        this.server = server;
        this.gateway = gateway;
    }

    @Override
    public EvacuationResult evacuate(String sourceWorld, EvacuationSettings settings) {
        World source = server.getWorld(sourceWorld);
        if (source == null) {
            return new EvacuationResult.Failed(
                    ResetFailureType.WORLD_NOT_LOADED,
                    0,
                    "The source world is no longer loaded in Bukkit.");
        }
        List<Player> players = List.copyOf(source.getPlayers());
        if (players.isEmpty()) {
            return new EvacuationResult.Success(0);
        }
        if (!settings.enabled()) {
            return new EvacuationResult.Failed(
                    ResetFailureType.EVACUATION_DISABLED,
                    players.size(),
                    "Evacuation is disabled while the world contains players.");
        }

        DestinationResult destination = gateway.resolveSafeDestination(settings.destination());
        if (destination instanceof DestinationResult.Unavailable unavailable) {
            return new EvacuationResult.Failed(
                    ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
                    players.size(),
                    unavailable.reason() + ": " + unavailable.message());
        }
        SafeLocation safe = ((DestinationResult.Available) destination).location();
        Location target = BukkitLocations.toBukkit(safe, server);
        if (target == null) {
            return new EvacuationResult.Failed(
                    ResetFailureType.EVACUATION_DESTINATION_UNAVAILABLE,
                    players.size(),
                    "Evacuation destination world is not loaded.");
        }
        int failedTeleports = 0;
        for (Player player : players) {
            if (!player.teleport(target.clone(), TeleportCause.PLUGIN)) {
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

    @Override
    public OptionalInt remainingPlayers(String sourceWorld) {
        World world = server.getWorld(sourceWorld);
        return world == null ? OptionalInt.empty() : OptionalInt.of(world.getPlayers().size());
    }
}
