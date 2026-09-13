package io.github.tamawish.rwr.config;

import java.util.Objects;

/** Platform-independent evacuation destination. */
public record EvacuationDestination(EvacuationDestinationType type, String target) {
  public EvacuationDestination {
    Objects.requireNonNull(type, "type");
    target = target == null ? "" : target.trim();
  }

  public static EvacuationDestination local(String target) {
    return new EvacuationDestination(EvacuationDestinationType.LOCAL_WORLD, target);
  }

  @Override
  public String toString() {
    return type.configKey() + (target.isBlank() ? "" : ": " + target);
  }
}
