package io.github.tamawish.rwr.config;

/**
 * Player-evacuation policy applied before regenerating a world.
 *
 * @param enabled whether players must be evacuated before reset
 * @param typedDestination configured transfer destination
 * @param timeoutSeconds maximum transfer and departure wait
 */
public record EvacuationSettings(
    boolean enabled, EvacuationDestination typedDestination, int timeoutSeconds) {
  public EvacuationSettings {
    java.util.Objects.requireNonNull(typedDestination, "typedDestination");
  }

  public EvacuationSettings(boolean enabled, String destination) {
    this(enabled, EvacuationDestination.local(destination), 30);
  }

  public EvacuationSettings(boolean enabled, EvacuationDestination destination) {
    this(enabled, destination, 30);
  }

  /** Legacy local-world accessor. New transports must inspect typedDestination(). */
  public String destination() {
    return typedDestination.target();
  }

  public EvacuationSettings withEnabled(boolean value) {
    return new EvacuationSettings(value, typedDestination, timeoutSeconds);
  }
}
