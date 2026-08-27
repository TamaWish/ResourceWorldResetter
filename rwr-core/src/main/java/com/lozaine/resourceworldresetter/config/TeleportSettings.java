package com.lozaine.resourceworldresetter.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TeleportSettings(
        boolean autoDiscover,
        boolean defaultEnabled,
        boolean showLocked,
        Map<String, TeleportDestinationSettings> worlds) {
    public TeleportSettings {
        worlds = Collections.unmodifiableMap(new LinkedHashMap<>(worlds));
    }
}
