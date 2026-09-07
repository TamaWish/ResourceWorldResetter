package io.github.tamawish.rwr.teleport;

import io.github.tamawish.rwr.config.TeleportDestinationSettings;
import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.multiverse.WorldSnapshot;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

  /**
   * Creates a catalog with provider-aware display-name resolution.
   *
   * @param gateway provider used to inspect registered worlds
   * @param resetAccess policy used to hide destinations during destructive reset phases
   * @param displayNames function resolving presentation names
   */
  public TeleportDestinationCatalog(
      WorldProvider gateway, ResetAccessPolicy resetAccess, Function<String, String> displayNames) {
    this.gateway = gateway;
    this.resetAccess = resetAccess;
    this.displayNames = displayNames;
  }

  /**
   * Lists configured and discovered destinations visible under a permission policy.
   *
   * @param settings active teleport settings
   * @param permissions permission policy for the requesting player
   * @return immutable destination list
   */
  public List<TeleportDestinationView> destinations(
      TeleportSettings settings, PermissionChecker permissions) {
    Catalog catalog = catalog(settings);
    LinkedHashSet<String> candidates = new LinkedHashSet<>();
    if (settings.autoDiscover()) {
      candidates.addAll(catalog.registered().keySet());
    }
    candidates.addAll(catalog.overrides().keySet());
    List<TeleportDestinationView> destinations = new ArrayList<>();
    for (String identity : candidates) {
      destination(settings, permissions, identity, catalog)
          .ifPresent(
              destination -> {
                if (destination.state() != TeleportDestinationState.LOCKED
                    || settings.showLocked()) {
                  destinations.add(destination);
                }
              });
    }
    return List.copyOf(destinations);
  }

  /**
   * Resolves one requested destination without loading its world.
   *
   * @param settings active teleport settings
   * @param permissions permission policy for the requesting player
   * @param requestedWorld requested provider world name or canonical identity
   * @return the destination when configured or discoverable
   */
  public Optional<TeleportDestinationView> destination(
      TeleportSettings settings, PermissionChecker permissions, String requestedWorld) {
    Catalog catalog = catalog(settings);
    return destination(
        settings, permissions, identity(requestedWorld, catalog.identities()), catalog);
  }

  private Optional<TeleportDestinationView> destination(
      TeleportSettings settings, PermissionChecker permissions, String identity, Catalog catalog) {
    WorldSnapshot snapshot = catalog.registered().get(identity);
    Map.Entry<String, TeleportDestinationSettings> override = catalog.overrides().get(identity);
    if (override == null && (!settings.autoDiscover() || snapshot == null)) {
      return Optional.empty();
    }

    String worldName = snapshot == null ? override.getKey() : snapshot.name();
    TeleportDestinationSettings effective =
        override == null
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
    return Optional.of(
        new TeleportDestinationView(
            worldName,
            displayNames.apply(worldName),
            effective.permission(),
            override != null,
            state));
  }

  /**
   * Returns a bounded page of destinations.
   *
   * @param settings active teleport settings
   * @param permissions permission policy for the requesting player
   * @param requestedPage requested zero-based page index
   * @return a page clamped to the available range
   */
  public TeleportPage page(
      TeleportSettings settings, PermissionChecker permissions, int requestedPage) {
    List<TeleportDestinationView> all = destinations(settings, permissions);
    int pageCount = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
    int start = Math.min(page * PAGE_SIZE, all.size());
    int end = Math.min(start + PAGE_SIZE, all.size());
    return new TeleportPage(all.subList(start, end), page, pageCount);
  }

  private Catalog catalog(TeleportSettings settings) {
    Map<String, WorldSnapshot> registered = new LinkedHashMap<>();
    Map<String, String> identities = new LinkedHashMap<>();
    for (WorldSnapshot world : gateway.registeredWorlds()) {
      String identity = normalize(world.identity());
      registered.put(identity, world);
      identities.put(normalize(world.name()), identity);
      identities.put(identity, identity);
    }
    Map<String, Map.Entry<String, TeleportDestinationSettings>> overrides = new LinkedHashMap<>();
    settings
        .worlds()
        .entrySet()
        .forEach(entry -> overrides.put(identity(entry.getKey(), identities), entry));
    return new Catalog(registered, overrides, identities);
  }

  private String identity(String name, Map<String, String> identities) {
    return identities.computeIfAbsent(
        normalize(name), ignored -> normalize(gateway.canonicalWorldName(name)));
  }

  private record Catalog(
      Map<String, WorldSnapshot> registered,
      Map<String, Map.Entry<String, TeleportDestinationSettings>> overrides,
      Map<String, String> identities) {}

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
