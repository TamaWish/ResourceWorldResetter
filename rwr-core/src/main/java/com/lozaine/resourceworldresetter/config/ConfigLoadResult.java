package com.lozaine.resourceworldresetter.config;

import java.util.List;

public record ConfigLoadResult(ConfigLoadStatus status, PluginSettings settings, List<ConfigIssue> issues) {
    public ConfigLoadResult {
        issues = List.copyOf(issues);
    }

    public boolean valid() {
        return status == ConfigLoadStatus.VALID && settings != null;
    }

    public static ConfigLoadResult valid(PluginSettings settings) {
        return new ConfigLoadResult(ConfigLoadStatus.VALID, settings, List.of());
    }

    public static ConfigLoadResult invalid(List<ConfigIssue> issues) {
        return new ConfigLoadResult(ConfigLoadStatus.INVALID, null, issues);
    }

    public static ConfigLoadResult migrationRequired(List<ConfigIssue> issues) {
        return new ConfigLoadResult(ConfigLoadStatus.MIGRATION_REQUIRED, null, issues);
    }
}
