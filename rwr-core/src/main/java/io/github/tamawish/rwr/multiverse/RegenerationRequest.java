package io.github.tamawish.rwr.multiverse;

import java.util.Objects;

public record RegenerationRequest(
        String worldName,
        SeedPolicy seedPolicy,
        Long fixedSeed,
        boolean keepWorldConfig,
        boolean keepGameRules,
        boolean keepWorldBorder) {
    public RegenerationRequest {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(seedPolicy, "seedPolicy");
        if (worldName.isBlank()) {
            throw new IllegalArgumentException("worldName must not be blank");
        }
        if (seedPolicy == SeedPolicy.FIXED && fixedSeed == null) {
            throw new IllegalArgumentException("fixedSeed is required for FIXED policy");
        }
        if (seedPolicy != SeedPolicy.FIXED && fixedSeed != null) {
            throw new IllegalArgumentException("fixedSeed is only valid for FIXED policy");
        }
    }
}
