package io.github.tamawish.rwr.bootstrap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

public final class DependencyValidator {
    public static final PluginVersion MINIMUM_MULTIVERSE_VERSION = new PluginVersion(5, 8, 0);
    public static final String DOWNLOAD_URL = "https://modrinth.com/plugin/multiverse-core";
    private static final int UNSUPPORTED_MULTIVERSE_MAJOR = 6;

    private final PluginManager pluginManager;

    public DependencyValidator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public ValidationResult validate() {
        Plugin multiverse = pluginManager.getPlugin("Multiverse-Core");
        if (multiverse == null || !multiverse.isEnabled()) {
            return ValidationResult.failure("Multiverse-Core is required and must be enabled first.");
        }

        PluginVersion installed;
        try {
            installed = PluginVersion.parse(pluginVersion(multiverse));
        } catch (IllegalArgumentException exception) {
            return ValidationResult.failure("Cannot parse the installed Multiverse-Core version: "
                    + pluginVersion(multiverse));
        }

        if (installed.compareTo(MINIMUM_MULTIVERSE_VERSION) < 0
                || installed.major() >= UNSUPPORTED_MULTIVERSE_MAJOR) {
            return ValidationResult.failure("Multiverse-Core " + MINIMUM_MULTIVERSE_VERSION
                    + " through 5.x is required; found " + installed + '.');
        }
        if (!MultiverseCoreApi.isLoaded()) {
            return ValidationResult.failure("Multiverse-Core is enabled but its typed API is not initialized.");
        }
        return ValidationResult.success(installed);
    }

    @SuppressWarnings("deprecation")
    private static String pluginVersion(Plugin plugin) {
        // Bukkit's PluginDescriptionFile API is used deliberately for Spigot-compatible linkage.
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
