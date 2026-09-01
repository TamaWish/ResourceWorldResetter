package io.github.tamawish.rwr.multiverse;

import io.github.tamawish.rwr.config.ConfigService;
import io.github.tamawish.rwr.world.WorldProvider;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mvplugins.multiverse.core.event.world.MVWorldCreatedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldImportedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldLoadedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldPropertyChangedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldRegeneratedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldRemovedEvent;
import org.mvplugins.multiverse.core.event.world.MVWorldUnloadedEvent;
import org.mvplugins.multiverse.core.world.MultiverseWorld;

public final class MultiverseLifecycleListener implements Listener {
    private final ConfigService configService;
    private final WorldProvider gateway;
    private final Logger logger;

    public MultiverseLifecycleListener(
            ConfigService configService,
            WorldProvider gateway,
            Logger logger) {
        this.configService = configService;
        this.gateway = gateway;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldCreated(MVWorldCreatedEvent event) {
        reconcile("created", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldImported(MVWorldImportedEvent event) {
        reconcile("imported", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoaded(MVWorldLoadedEvent event) {
        reconcile("loaded", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnloaded(MVWorldUnloadedEvent event) {
        reconcile("unloaded", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldRemoved(MVWorldRemovedEvent event) {
        reconcile("removed", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldRegenerated(MVWorldRegeneratedEvent event) {
        reconcile("regenerated", event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldPropertyChanged(MVWorldPropertyChangedEvent<?> event) {
        logger.fine(() -> "Observed Multiverse world property change: "
                + event.getWorld().getName() + "." + event.getName());
    }

    private void reconcile(String action, MultiverseWorld world) {
        ConfigService.ReconciliationResult result = configService.reconcileWorldStates(gateway);
        logger.fine(() -> "Observed Multiverse world " + action + ": " + world.getName()
                + "; reconciled " + result.changedWorlds() + " RWR state(s) in memory.");
    }
}
