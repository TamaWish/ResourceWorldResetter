package com.lozaine.resourceworldresetter.config;

import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record PluginSettings(
        int configVersion,
        ZoneId timezone,
        String defaultHubWorld,
        ResetPolicySettings resetPolicy,
        Map<String, ManagedWorldSettings> worlds,
        TeleportSettings teleport) {
    public PluginSettings {
        worlds = Collections.unmodifiableMap(new LinkedHashMap<>(worlds));
    }

    public Optional<ManagedWorldSettings> world(String id) {
        return Optional.ofNullable(worlds.get(id));
    }
}
