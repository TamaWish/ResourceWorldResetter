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
            getLogger().severe("[RWR] [ERROR] Worlds is binary-incompatible with RWR-Paper-Folia 5: "
                    + error.getMessage());
            getLogger().severe("[RWR] [ACTION] Install Worlds 4.4.0+ for Paper/Purpur/Folia: "
                    + "https://modrinth.com/plugin/worlds-1");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
        getLogger().info("[RWR] [INFO] ResourceWorldResetter-Paper-Folia disabled.");
    }
}
