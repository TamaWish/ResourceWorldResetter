package io.github.tamawish.rwr;

import io.github.tamawish.rwr.bootstrap.PluginBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceWorldResetterPlugin extends JavaPlugin {
    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        try {
            bootstrap = new PluginBootstrap(this);
            if (!bootstrap.enable()) {
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (LinkageError error) {
            getLogger().severe("[RWR] [ERROR] Multiverse-Core is binary-incompatible with RWR-Spigot 5: "
                    + error.getMessage());
            getLogger().severe("[RWR] [ACTION] Install Multiverse-Core 5.8.0 through 5.x: "
                    + "https://modrinth.com/plugin/multiverse-core");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
    }
}
