package com.lozaine.resourceworldresetter.teleport;

import com.lozaine.resourceworldresetter.config.TeleportDestinationSettings;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import com.lozaine.resourceworldresetter.reset.ResetAccessPolicy;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Produces immutable player-facing destinations without loading any world. */
public final class TeleportDestinationCatalog {
    public static final int PAGE_SIZE = 45;
    public static final String WILDCARD_PERMISSION = "rwr.teleport.world.*";

    private final WorldProvider gateway;
    private final ResetAccessPolicy resetAccess;
    private final Function<String, String> displayNames;

    public TeleportDestinationCatalog(WorldProvider gateway, ResetAccessPolicy resetAccess) {
        this(gateway, resetAccess, name -> name);
    }

    public TeleportDestinationCatalog(
            WorldProvider gateway,
            ResetAccessPolicy resetAccess,
            Function<String, String> displayNames) {
        this.gateway = gateway;
        this.resetAccess = resetAccess;
        this.displayNames = displayNames;
    }

    public List<TeleportDestinationView> destinations(
            TeleportSettings settings, PermissionChecker permissions) {
        List<TeleportDestinationView> destinations = new ArrayList<>();
        for (String worldName : candidateNames(settings).values()) {
            destination(settings, permissions, worldName).ifPresent(destination -> {
                if (destination.state() != TeleportDestinationState.LOCKED || settings.showLocked()) {
                    destinations.add(destination);
                }
            });
        }
        return List.copyOf(destinations);
    }

    public Optional<TeleportDestinationView> destination(
            TeleportSettings settings, PermissionChecker permissions, String requestedWorld) {
        Map<String, WorldSnapshot> registered = registeredByName();
        Map<String, Map.Entry<String, TeleportDestinationSettings>> overrides = overridesByName(settings);
        String normalized = normalize(requestedWorld);
        WorldSnapshot snapshot = registered.get(normalized);
        Map.Entry<String, TeleportDestinationSettings> override = overrides.get(normalized);
        if (override == null && (!settings.autoDiscover() || snapshot == null)) {
            return Optional.empty();
        }

        String worldName = snapshot == null ? override.getKey() : snapshot.name();
        TeleportDestinationSettings effective = override == null
                ? new TeleportDestinationSettings(settings.defaultEnabled(), null)
                : override.getValue();
        if (!effective.enabled()) {
            return Optional.empty();
        }

        TeleportDestinationState state;
        if (snapshot == null || !snapshot.loaded()) {
            state = TeleportDestinationState.UNAVAILABLE;
        } else if (resetAccess.blocksIncomingRwrTeleport(worldName)) {
            state = TeleportDestinationState.RESETTING;
        } else if (!hasAccess(effective.permission(), permissions)) {
            state = TeleportDestinationState.LOCKED;
        } else {
            state = TeleportDestinationState.AVAILABLE;
        }
        return Optional.of(new TeleportDestinationView(
                worldName,
                displayNames.apply(worldName),
                effective.permission(),
                override != null,
                state));
    }

    public TeleportPage page(TeleportSettings settings, PermissionChecker permissions, int requestedPage) {
        List<TeleportDestinationView> all = destinations(settings, permissions);
        int pageCount = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = Math.min(page * PAGE_SIZE, all.size());
        int end = Math.min(start + PAGE_SIZE, all.size());
        return new TeleportPage(all.subList(start, end), page, pageCount);
    }

    private LinkedHashMap<String, String> candidateNames(TeleportSettings settings) {
        LinkedHashMap<String, String> candidates = new LinkedHashMap<>();
        if (settings.autoDiscover()) {
            gateway.registeredWorlds().forEach(world -> candidates.put(normalize(world.name()), world.name()));
        }
        settings.worlds().keySet().forEach(name -> candidates.putIfAbsent(normalize(name), name));
        return candidates;
    }

    private Map<String, WorldSnapshot> registeredByName() {
        Map<String, WorldSnapshot> registered = new LinkedHashMap<>();
        gateway.registeredWorlds().forEach(world -> registered.put(normalize(world.name()), world));
        return registered;
    }

    private static Map<String, Map.Entry<String, TeleportDestinationSettings>> overridesByName(
            TeleportSettings settings) {
        Map<String, Map.Entry<String, TeleportDestinationSettings>> overrides = new LinkedHashMap<>();
        settings.worlds().entrySet().forEach(entry -> overrides.put(normalize(entry.getKey()), entry));
        return overrides;
    }

    private static boolean hasAccess(String permission, PermissionChecker checker) {
        return permission == null
                || permission.isBlank()
                || checker.hasPermission(WILDCARD_PERMISSION)
                || checker.hasPermission(permission);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
