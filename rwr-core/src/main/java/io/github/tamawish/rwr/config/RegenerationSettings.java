package io.github.tamawish.rwr.config;

import io.github.tamawish.rwr.multiverse.SeedPolicy;

public record RegenerationSettings(
        SeedPolicy seedPolicy,
        Long fixedSeed,
        boolean keepWorldConfig,
        boolean keepGameRules,
        boolean keepWorldBorder) {}
