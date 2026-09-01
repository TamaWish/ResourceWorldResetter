package io.github.tamawish.rwr.world;

import org.bukkit.Location;
import org.bukkit.World;

public final class BukkitLocations {
    private BukkitLocations() {}

    public static SafeLocation from(Location location) {
        World world = location.getWorld();
        String name = world == null ? "" : world.getName();
        return new SafeLocation(
                name, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static Location toBukkit(SafeLocation location, World world) {
        return new Location(
                world, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
    }

    public static Location toBukkit(SafeLocation location, org.bukkit.Server server) {
        World world = server.getWorld(location.worldName());
        if (world == null) {
            return null;
        }
        return toBukkit(location, world);
    }
}
