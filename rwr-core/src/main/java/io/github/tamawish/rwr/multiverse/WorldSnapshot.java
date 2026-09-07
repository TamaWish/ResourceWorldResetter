package io.github.tamawish.rwr.multiverse;

/**
 * Provider-neutral snapshot used to verify world identity and retained settings.
 *
 * @param identity stable provider identity
 * @param name provider-specific world name
 * @param alias provider display alias
 * @param loaded whether the world is currently loaded
 * @param environment world environment identifier
 * @param seed current world seed
 * @param generator configured chunk generator
 * @param generatorSettings serialized generator settings
 * @param worldType configured world type
 * @param structures whether structures are enabled
 * @param adjustSpawn whether the provider adjusts unsafe spawns
 * @param spawn serialized spawn location
 */
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
