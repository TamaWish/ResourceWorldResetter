package com.lozaine.resourceworldresetter.multiverse;

public record WorldSnapshot(
        String identity,
        String name,
        String alias,
        boolean loaded,
        String environment,
        long seed,
        String generator,
        String generatorSettings,
        String worldType,
        boolean structures,
        boolean adjustSpawn,
        String spawn) {}
