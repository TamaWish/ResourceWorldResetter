package com.lozaine.resourceworldresetter.worlds;

import com.lozaine.resourceworldresetter.config.ConfigService;
import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.gui.GuiConfigurationEditor;
import com.lozaine.resourceworldresetter.gui.GuiEditResult;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.thenextlvl.worlds.event.WorldDeleteEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Sync RWR config with Worlds lifecycle.
 * When a world is deleted via Worlds, drop matching managed RWR entries.
 * Unmanaged (hub/teleport-only) entries are never auto-removed.
 */
public final class WorldsLifecycleListener implements Listener {
    private final ConfigService configService;
    private final WorldsWorldProvider gateway;
    private final GuiConfigurationEditor editor;
    private final Logger logger;

    public WorldsLifecycleListener(ConfigService configService, WorldsWorldProvider gateway, Logger logger) {
        this.configService = configService;
        this.gateway = gateway;
        this.editor = new GuiConfigurationEditor(configService, gateway);
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldDelete(WorldDeleteEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }

        Key resolved = null;
        try {
            resolved = gateway.keys().resolveKey(world);
        } catch (Throwable throwable) {
            logger.fine("Could not resolve key on delete for " + world.getName() + ": " + throwable.getMessage());
        }

        String bukkitName = world.getName();
        String resolvedStr = resolved != null ? resolved.asString() : null;

        List<String> toRemove = new ArrayList<>();
        for (ManagedWorldSettings settings : configService.current().worlds().values()) {
            if (!settings.managed()) {
                continue;
            }
            if (matches(settings, bukkitName, resolvedStr, resolved)) {
                toRemove.add(settings.id());
            }
        }

        if (toRemove.isEmpty()) {
            return;
        }

        for (String id : toRemove) {
            GuiEditResult result = editor.removeWorld(id);
            if (result.accepted()) {
                logger.info("Removed managed RWR entry '" + id
                        + "' because Worlds deleted world '" + bukkitName
                        + "'" + (resolvedStr != null ? " (key=" + resolvedStr + ")" : ""));
            } else {
                logger.warning("Failed to remove managed RWR entry '" + id + "' after Worlds delete: "
                        + result.message());
            }
        }

        ConfigService.ReconciliationResult reconciled = configService.reconcileWorldStates(gateway);
        logger.fine(() -> "Reconciled " + reconciled.changedWorlds() + " RWR state(s) after Worlds delete.");
    }

    private static boolean matches(
            ManagedWorldSettings settings, String bukkitName, String resolvedStr, Key resolved) {
        String key = settings.multiverseWorld();
        String id = settings.id();
        if (key == null || key.isBlank()) {
            return false;
        }

        if (resolvedStr != null && key.equalsIgnoreCase(resolvedStr)) {
            return true;
        }
        if (key.equalsIgnoreCase(bukkitName)) {
            return true;
        }
        if (id != null && id.equalsIgnoreCase(bukkitName)) {
            return true;
        }

        String keyUnderscore = key.replace(':', '_');
        if (keyUnderscore.equalsIgnoreCase(bukkitName)) {
            return true;
        }
        if (resolvedStr != null && resolvedStr.replace(':', '_').equalsIgnoreCase(keyUnderscore)) {
            return true;
        }

        if (key.contains(":")) {
            String value = key.substring(key.indexOf(':') + 1);
            if (value.equalsIgnoreCase(bukkitName)) {
                return true;
            }
        }

        if (resolved != null && id != null && resolved.value().equalsIgnoreCase(id)
                && resolvedStr != null && key.equalsIgnoreCase(resolvedStr)) {
            return true;
        }

        return false;
    }
}
