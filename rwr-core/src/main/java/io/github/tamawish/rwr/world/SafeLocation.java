package io.github.tamawish.rwr.world;

/**
 * Provider-neutral safe location including optional orientation.
 *
 * @param worldName provider-specific world name
 * @param x horizontal x coordinate
 * @param y vertical coordinate
 * @param z horizontal z coordinate
 * @param yaw horizontal orientation
 * @param pitch vertical orientation
 */
public record SafeLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
  /** Validates that the location identifies a provider world. */
  public SafeLocation {
    if (worldName == null || worldName.isBlank()) {
      throw new IllegalArgumentException("worldName is required");
    }
  }

  public SafeLocation(String worldName, double x, double y, double z) {
    this(worldName, x, y, z, 0f, 0f);
  }
}
