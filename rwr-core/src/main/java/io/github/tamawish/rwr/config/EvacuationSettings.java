package io.github.tamawish.rwr.config;

/**
 * Player-evacuation policy applied before regenerating a world.
 *
 * @param enabled whether players must be evacuated before reset
 * @param destination configured safe destination world, or {@code null} for the default
 */
public record EvacuationSettings(boolean enabled, String destination) {}
