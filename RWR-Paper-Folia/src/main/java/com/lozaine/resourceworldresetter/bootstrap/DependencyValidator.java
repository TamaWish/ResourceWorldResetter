package com.lozaine.resourceworldresetter.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class DependencyValidator {
    public static final PluginVersion MINIMUM_WORLDS_VERSION = new PluginVersion(4, 4, 0);

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
        return validateVersion(pluginVersion(worlds));
    }

    static ValidationResult validateVersion(String version) {
        PluginVersion installed;
        try {
            installed = PluginVersion.parse(version);
        } catch (IllegalArgumentException exception) {
            return ValidationResult.failure("Cannot parse the installed Worlds version: " + version);
        }
        if (installed.compareTo(MINIMUM_WORLDS_VERSION) < 0) {
            return ValidationResult.failure(
                    "Worlds " + MINIMUM_WORLDS_VERSION + " or newer is required; found " + installed + '.');
        }
        return ValidationResult.success(installed);
    }

    @SuppressWarnings("deprecation")
    private static String pluginVersion(Plugin plugin) {
        return plugin.getDescription().getVersion();
    }

    public record ValidationResult(boolean compatible, PluginVersion installedVersion, String message) {
        static ValidationResult success(PluginVersion version) {
            return new ValidationResult(true, version, "compatible");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, null, message);
        }
    }
}
