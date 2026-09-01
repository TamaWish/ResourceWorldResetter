package io.github.tamawish.rwr.teleport;

public record TeleportDestinationView(
        String worldName,
        String displayName,
        String permission,
        boolean explicitOverride,
        TeleportDestinationState state) {}
