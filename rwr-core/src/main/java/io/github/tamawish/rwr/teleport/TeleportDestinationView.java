package io.github.tamawish.rwr.teleport;

/**
 * Immutable player-facing projection of a teleport destination.
 *
 * @param worldName provider-specific world name
 * @param displayName configured presentation name
 * @param permission permission required to use the destination
 * @param explicitOverride whether configuration explicitly defines this destination
 * @param state current availability state
 */
public record TeleportDestinationView(
    String worldName,
    String displayName,
    String permission,
    boolean explicitOverride,
    TeleportDestinationState state) {}
