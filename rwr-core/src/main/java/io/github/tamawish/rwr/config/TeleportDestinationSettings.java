package io.github.tamawish.rwr.config;

/**
 * Per-world override for player teleport discovery.
 *
 * @param enabled whether the destination is shown when available
 * @param permission permission required to use the destination, or {@code null} when unrestricted
 */
public record TeleportDestinationSettings(boolean enabled, String permission) {}
