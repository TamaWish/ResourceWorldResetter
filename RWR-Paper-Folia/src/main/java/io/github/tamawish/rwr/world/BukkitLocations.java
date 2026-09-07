package io.github.tamawish.rwr.world;

import org.bukkit.Location;
import org.bukkit.World;

/** Converts between Bukkit locations and the provider-neutral core representation. */
public final class BukkitLocations {
  private BukkitLocations() {}

  /**
   * Converts a Bukkit location to its provider-neutral representation.
   *
   * @param location Bukkit location with a world
   * @return provider-neutral location
   */
  public static SafeLocation from(Location location) {
    World world = location.getWorld();
    String name = world == null ? "" : world.getName();
    return new SafeLocation(
        name,
        location.getX(),
        location.getY(),
        location.getZ(),
        location.getYaw(),
        location.getPitch());
  }

  public static Location toBukkit(SafeLocation location, World world) {
    return new Location(
        world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
  }

  /**
   * Resolves and converts a provider-neutral location through a server.
   *
   * @param location provider-neutral location
   * @param server server used to resolve the destination world
   * @return Bukkit location, or {@code null} when the world is unavailable
   */
  public static Location toBukkit(SafeLocation location, org.bukkit.Server server) {
    World world = server.getWorld(location.worldName());
    if (world == null) {
      return null;
    }
    return toBukkit(location, world);
  }
}
