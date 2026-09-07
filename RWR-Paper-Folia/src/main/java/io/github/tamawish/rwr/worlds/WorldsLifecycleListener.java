package io.github.tamawish.rwr.worlds;

import io.github.tamawish.rwr.config.ConfigService;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.gui.GuiConfigurationEditor;
import io.github.tamawish.rwr.gui.GuiEditResult;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
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
 * Sync RWR config with Worlds lifecycle. When a world is deleted via Worlds, drop matching managed
 * RWR entries. Unmanaged (hub/teleport-only) entries are never auto-removed.
 */
public final class WorldsLifecycleListener implements Listener {
  private final ConfigService configService;
  private final WorldsWorldProvider gateway;
  private final GuiConfigurationEditor editor;
  private final Logger logger;
  private final ResetAccessPolicy resetAccess;

  /**
   * Creates a listener that reconciles RWR state with Worlds lifecycle events.
   *
   * @param configService active configuration service
   * @param gateway Worlds-backed provider
   * @param logger destination for reconciliation failures
   * @param resetAccess active reset access policy
   */
  public WorldsLifecycleListener(
      ConfigService configService,
      WorldsWorldProvider gateway,
      Logger logger,
      ResetAccessPolicy resetAccess) {
    this.configService = configService;
    this.gateway = gateway;
    this.editor = new GuiConfigurationEditor(configService, gateway);
    this.logger = logger;
    this.resetAccess = resetAccess;
  }

  /**
   * Removes matching managed configuration after a Worlds deletion.
   *
   * @param event Worlds deletion event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldDelete(WorldDeleteEvent event) {
    World world = event.getWorld();
    if (world == null || resetAccess.blocksIncomingRwrTeleport(world.getName())) {
      return;
    }

    Key resolved = null;
    try {
      resolved = gateway.keys().resolveKey(world);
    } catch (Throwable throwable) {
      logger.fine(
          "Could not resolve key on delete for " + world.getName() + ": " + throwable.getMessage());
    }

    String bukkitName = world.getName();
    String resolvedStr = resolved != null ? resolved.asString() : null;

    List<String> toRemove = new ArrayList<>();
    for (ManagedWorldSettings settings : configService.current().worlds().values()) {
      if (!settings.managed()) {
        continue;
      }
      if (matches(settings.multiverseWorld(), bukkitName, resolvedStr)) {
        toRemove.add(settings.id());
      }
    }

    if (toRemove.isEmpty()) {
      return;
    }

    for (String id : toRemove) {
      GuiEditResult result = editor.removeWorld(id);
      if (result.accepted()) {
        logger.info(
            "Removed managed RWR entry '"
                + id
                + "' because Worlds deleted world '"
                + bukkitName
                + "'"
                + (resolvedStr != null ? " (key=" + resolvedStr + ")" : ""));
      } else {
        logger.warning(
            "Failed to remove managed RWR entry '"
                + id
                + "' after Worlds delete: "
                + result.message());
      }
    }

    ConfigService.ReconciliationResult reconciled = configService.reconcileWorldStates(gateway);
    logger.fine(
        () -> "Reconciled " + reconciled.changedWorlds() + " RWR state(s) after Worlds delete.");
  }

  static boolean matches(String configuredWorld, String bukkitName, String resolvedKey) {
    if (configuredWorld == null || configuredWorld.isBlank()) {
      return false;
    }
    return configuredWorld.equalsIgnoreCase(bukkitName)
        || (resolvedKey != null && configuredWorld.equalsIgnoreCase(resolvedKey))
        || configuredWorld.replace(':', '_').equalsIgnoreCase(bukkitName);
  }
}
