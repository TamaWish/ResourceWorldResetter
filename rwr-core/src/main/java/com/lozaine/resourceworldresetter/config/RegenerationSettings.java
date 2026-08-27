package com.lozaine.resourceworldresetter.config;

import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;

public record RegenerationSettings(
        SeedPolicy seedPolicy,
        Long fixedSeed,
        boolean keepWorldConfig,
        boolean keepGameRules,
        boolean keepWorldBorder) {}
