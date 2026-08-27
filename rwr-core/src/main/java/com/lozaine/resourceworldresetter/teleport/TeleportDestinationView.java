package com.lozaine.resourceworldresetter.teleport;

public record TeleportDestinationView(
        String worldName,
        String displayName,
        String permission,
        boolean explicitOverride,
        TeleportDestinationState state) {}
