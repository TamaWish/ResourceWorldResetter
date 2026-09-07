package io.github.tamawish.rwr.gui;

import io.github.tamawish.rwr.message.MessageService;
import io.github.tamawish.rwr.teleport.TeleportAttempt;
import io.github.tamawish.rwr.teleport.TeleportDestinationState;
import io.github.tamawish.rwr.teleport.TeleportDestinationView;
import io.github.tamawish.rwr.teleport.TeleportPage;
import io.github.tamawish.rwr.teleport.TeleportService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Player-facing 45-destination selector with policy revalidation on click. */
public final class PlayerTeleportGui implements Listener {
  private final TeleportService teleports;
  private final MessageService messages;

  public PlayerTeleportGui(TeleportService teleports, MessageService messages) {
    this.teleports = teleports;
    this.messages = messages;
  }

  /**
   * Opens the first destination page.
   *
   * @param player player for whom the page is opened
   */
  public void open(Player player) {
    openPage(player, 0);
  }

  /**
   * Routes clicks within a teleport inventory.
   *
   * @param event inventory click to route or cancel
   */
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!(top.getHolder() instanceof TeleportHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= top.getSize()) {
      return;
    }
    TeleportAction action = holder.action(event.getRawSlot());
    if (action == null) {
      return;
    }
    switch (action.type()) {
      case DESTINATION -> {
        player.closeInventory();
        TeleportAttempt attempt = teleports.teleport(player, action.worldName());
        messages.send(
            player,
            attempt.successful() ? "gui.success" : "gui.failure",
            "message",
            attempt.message());
      }
      case PAGE -> openPage(player, action.page());
      case CLOSE -> player.closeInventory();
      default -> throw new AssertionError("Unhandled teleport action: " + action.type());
    }
  }

  /**
   * Prevents item dragging into a teleport inventory.
   *
   * @param event inventory drag to inspect and potentially cancel
   */
  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof TeleportHolder)) {
      return;
    }
    int topSize = event.getView().getTopInventory().getSize();
    if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
      event.setCancelled(true);
    }
  }

  private void openPage(Player player, int requestedPage) {
    TeleportPage page = teleports.page(player::hasPermission, requestedPage);
    TeleportHolder holder = new TeleportHolder();
    Component title = Component.text("RWR Worlds " + (page.page() + 1) + '/' + page.pageCount());
    Inventory inventory =
        Bukkit.createInventory(
            holder, 54, LegacyComponentSerializer.legacySection().serialize(title));
    holder.attach(inventory);

    int slot = 0;
    for (TeleportDestinationView destination : page.destinations()) {
      holder.add(
          slot++,
          destinationItem(destination),
          new TeleportAction(ActionType.DESTINATION, destination.worldName(), 0));
    }
    if (page.hasPrevious()) {
      holder.add(
          45,
          item(Material.ARROW, "&ePrevious page", "Page " + page.page()),
          new TeleportAction(ActionType.PAGE, "", page.page() - 1));
    }
    holder.add(49, item(Material.BARRIER, "&cClose"), new TeleportAction(ActionType.CLOSE, "", 0));
    if (page.hasNext()) {
      holder.add(
          53,
          item(Material.ARROW, "&eNext page", "Page " + (page.page() + 2)),
          new TeleportAction(ActionType.PAGE, "", page.page() + 1));
    }
    player.openInventory(inventory);
  }

  private static ItemStack destinationItem(TeleportDestinationView destination) {
    final Material material;
    final String status;
    switch (destination.state()) {
      case AVAILABLE -> {
        material = Material.ENDER_PEARL;
        status = "&aAvailable — click to teleport";
      }
      case LOCKED -> {
        material = Material.IRON_BARS;
        status = "&cLocked — permission required";
      }
      case UNAVAILABLE -> {
        material = Material.BARRIER;
        status = "&cUnavailable — world is not loaded";
      }
      case RESETTING -> {
        material = Material.CLOCK;
        status = "&eUnavailable — reset in progress";
      }
      default -> throw new AssertionError("Unhandled destination state: " + destination.state());
    }
    final String color = destination.state() == TeleportDestinationState.AVAILABLE ? "&a" : "&c";
    List<String> lore = new ArrayList<>();
    lore.add("World: " + destination.worldName());
    var loadedWorld = Bukkit.getWorld(destination.worldName());
    lore.add("Players: " + (loadedWorld == null ? "unavailable" : loadedWorld.getPlayers().size()));
    lore.add(status);
    if (destination.permission() != null && !destination.permission().isBlank()) {
      lore.add("Permission: " + destination.permission());
    }
    return item(material, color + destination.displayName(), lore.toArray(String[]::new));
  }

  @SuppressWarnings("deprecation")
  private static ItemStack item(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(color(name));
    List<String> coloredLore = new ArrayList<>();
    for (String line : lore) {
      coloredLore.add(color("&7" + line));
    }
    meta.setLore(coloredLore);
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    item.setItemMeta(meta);
    return item;
  }

  private static String color(String value) {
    return ChatColor.translateAlternateColorCodes('&', value);
  }

  private enum ActionType {
    DESTINATION,
    PAGE,
    CLOSE
  }

  private record TeleportAction(ActionType type, String worldName, int page) {}

  private static final class TeleportHolder implements InventoryHolder {
    private final Map<Integer, TeleportAction> actions = new HashMap<>();
    private Inventory inventory;

    private void attach(Inventory value) {
      inventory = value;
    }

    private void add(int slot, ItemStack item, TeleportAction action) {
      inventory.setItem(slot, item);
      actions.put(slot, action);
    }

    private TeleportAction action(int slot) {
      return actions.get(slot);
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }
}
