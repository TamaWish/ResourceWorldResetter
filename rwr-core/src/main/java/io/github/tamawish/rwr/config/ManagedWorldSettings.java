package io.github.tamawish.rwr.config;

import java.util.List;
import java.util.Objects;

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
