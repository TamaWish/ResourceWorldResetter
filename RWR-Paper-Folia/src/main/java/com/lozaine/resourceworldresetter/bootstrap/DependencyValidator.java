package com.lozaine.resourceworldresetter.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class DependencyValidator {
    private final PluginManager pluginManager;

    public DependencyValidator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public ValidationResult validate() {
        Plugin worlds = pluginManager.getPlugin("Worlds");
        if (worlds == null || !worlds.isEnabled()) {
            return ValidationResult.failure(
                    "Worlds (TheNextLvl) is required and must be enabled first. "
                            + "Download: https://modrinth.com/plugin/worlds");
        }
        String version = pluginVersion(worlds);
        return ValidationResult.success(version);
    }

    @SuppressWarnings("deprecation")
    private static String pluginVersion(Plugin plugin) {
        return plugin.getDescription().getVersion();
    }

    public record ValidationResult(boolean compatible, String installedVersion, String message) {
        static ValidationResult success(String version) {
            return new ValidationResult(true, version, "compatible");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, null, message);
        }
    }
}
