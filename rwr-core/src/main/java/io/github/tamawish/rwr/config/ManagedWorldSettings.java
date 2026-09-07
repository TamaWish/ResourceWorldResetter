package io.github.tamawish.rwr.config;

import java.util.List;
import java.util.Objects;

/**
 * Complete configuration and derived operational state for one managed world.
 *
 * @param id stable configuration identifier
 * @param multiverseWorld provider-specific world name or key
 * @param displayName player-facing world name
 * @param enabled whether scheduled resets are enabled
 * @param managed whether RWR is allowed to manage the world
 * @param schedule reset schedule
 * @param warnings warning offsets in minutes
 * @param regeneration world-regeneration policy
 * @param evacuation player-evacuation policy
 * @param state current derived operational state
 */
public record ManagedWorldSettings(
    String id,
    String multiverseWorld,
    String displayName,
    boolean enabled,
    boolean managed,
    ScheduleSettings schedule,
    List<Integer> warnings,
    RegenerationSettings regeneration,
    EvacuationSettings evacuation,
    WorldOperationalState state) {
  public ManagedWorldSettings {
    warnings = List.copyOf(warnings);
    Objects.requireNonNull(state, "state");
  }

  public boolean canReset() {
    return enabled && managed && state == WorldOperationalState.MANAGED;
  }

  /**
   * Copies these settings with a newly derived state.
   *
   * @param newState operational state to assign
   * @return settings containing the replacement state
   */
  public ManagedWorldSettings withState(WorldOperationalState newState) {
    return new ManagedWorldSettings(
        id,
        multiverseWorld,
        displayName,
        enabled,
        managed,
        schedule,
        warnings,
        regeneration,
        evacuation,
        newState);
  }
}
