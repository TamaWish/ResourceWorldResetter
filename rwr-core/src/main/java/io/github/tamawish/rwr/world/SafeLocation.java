package io.github.tamawish.rwr.world;

/**
 * Platform-neutral safe spawn / teleport destination.
 */
public record SafeLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
    public SafeLocation {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("worldName is required");
        }
    }

    public SafeLocation(String worldName, double x, double y, double z) {
        this(worldName, x, y, z, 0f, 0f);
    }
}
