package com.lozaine.resourceworldresetter;

import com.lozaine.resourceworldresetter.bootstrap.PluginBootstrap;
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
            getLogger().severe("The installed Worlds API is binary-incompatible with ResourceWorldResetter-Paper-Folia 5: "
                    + error.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.disable();
        }
        getLogger().info("ResourceWorldResetter-Paper-Folia disabled.");
    }
}
