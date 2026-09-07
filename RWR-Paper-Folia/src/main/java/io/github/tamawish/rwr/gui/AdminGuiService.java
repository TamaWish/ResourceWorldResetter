package io.github.tamawish.rwr.gui;

import io.github.tamawish.rwr.bootstrap.UpdateService;
import io.github.tamawish.rwr.config.EvacuationSettings;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.config.RegenerationSettings;
import io.github.tamawish.rwr.config.ResetPolicySettings;
import io.github.tamawish.rwr.config.ScheduleSettings;
import io.github.tamawish.rwr.config.ScheduleType;
import io.github.tamawish.rwr.config.TeleportDestinationSettings;
import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.config.WorldDisplayNames;
import io.github.tamawish.rwr.history.ResetHistoryEntry;
import io.github.tamawish.rwr.message.MessageService;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import io.github.tamawish.rwr.multiverse.WorldSnapshot;
import io.github.tamawish.rwr.reset.ResetCoordinator;
import io.github.tamawish.rwr.scheduler.ScheduleManager;
import io.github.tamawish.rwr.world.WorldProvider;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

/** All M5 admin screens and the single action router that owns them. */
public final class AdminGuiService implements Listener {
  private static final LegacyComponentSerializer LEGACY_TEXT =
      LegacyComponentSerializer.legacyAmpersand();
  private static final DateTimeFormatter HISTORY_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
  private static final int[] CONTENT_SLOTS = {
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34,
    37, 38, 39, 40, 41, 42, 43
  };
  private static final int PAGE_SIZE = CONTENT_SLOTS.length;
  private final JavaPlugin plugin;
  private final io.github.tamawish.rwr.config.ConfigService configs;
  private final WorldProvider gateway;
  private final ResetCoordinator resets;
  private final ScheduleManager schedules;
  private final GuiConfigurationEditor editor;
  private final GuiInputService input;
  private final MessageService messages;
  private final UpdateService updates;

  /**
   * Creates the administrative GUI router.
   *
   * @param plugin owning plugin
   * @param configs active configuration service
   * @param gateway world provider
   * @param resets reset workflow coordinator
   * @param schedules reset schedule manager
   * @param input text-input service
   * @param messages localized message service
   * @param updates update-check settings service
   */
  public AdminGuiService(
      JavaPlugin plugin,
      io.github.tamawish.rwr.config.ConfigService configs,
      WorldProvider gateway,
      ResetCoordinator resets,
      ScheduleManager schedules,
      GuiInputService input,
      MessageService messages,
      UpdateService updates) {
    this.plugin = plugin;
    this.configs = configs;
    this.gateway = gateway;
    this.resets = resets;
    this.schedules = schedules;
    this.editor = new GuiConfigurationEditor(configs, gateway);
    this.input = input;
    this.messages = messages;
    this.updates = updates;
  }

  /**
   * Opens the administrative dashboard.
   *
   * @param player administrator for whom the dashboard is opened
   */
  public void open(Player player) {
    openDashboard(player, 0);
  }

  /**
   * Routes clicks within an administrative inventory.
   *
   * @param event inventory click to route or cancel
   */
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!(top.getHolder() instanceof AdminHolder holder)) {
      return;
    }
    event.setCancelled(true);
    if (!(event.getWhoClicked() instanceof Player player)
        || event.getRawSlot() < 0
        || event.getRawSlot() >= top.getSize()) {
      return;
    }
    GuiAction action = holder.action(event.getRawSlot());
    if (action != null) {
      route(player, action);
    }
  }

  /**
   * Prevents item dragging into an administrative inventory.
   *
   * @param event inventory drag to inspect and potentially cancel
   */
  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof AdminHolder)) {
      return;
    }
    int topSize = event.getView().getTopInventory().getSize();
    if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
      event.setCancelled(true);
    }
  }

  private void route(Player player, GuiAction action) {
    switch (action.type()) {
      case DASHBOARD -> openDashboard(player, action.number());
      case ADD_WORLD -> openAddWorld(player, action.number());
      case WORLD -> openWorld(player, action.value());
      case ADD_SELECTED ->
          localizedResult(
              player,
              editor.addWorld(action.value()),
              "gui.world-added",
              () -> openWorldByName(player, action.value()));
      case GLOBAL -> openGlobal(player);
      case TELEPORTS -> openTeleports(player, action.number());
      case TELEPORT_WORLD -> openTeleportWorld(player, action.value());
      case HISTORY -> openHistory(player);
      case RELOAD -> reload(player);
      case EDIT_DISPLAY ->
          text(
              player,
              tr("gui.input.display-name"),
              world(action.value()).displayName(),
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old,
                              required(value, tr("gui.field.display-name")),
                              null,
                              null,
                              null,
                              null,
                              null)));
      case TOGGLE_ENABLED ->
          editWorld(
              player,
              action.value(),
              old ->
                  GuiConfigurationEditor.copyWorld(
                      old, null, !old.enabled(), null, null, null, null));
      case CYCLE_SCHEDULE ->
          editWorld(
              player,
              action.value(),
              old -> {
                ScheduleType[] values = ScheduleType.values();
                ScheduleType next = values[(old.schedule().type().ordinal() + 1) % values.length];
                ScheduleSettings schedule = defaultsFor(next, old.schedule());
                return GuiConfigurationEditor.copyWorld(
                    old, null, null, schedule, null, null, null);
              });
      case EDIT_TIME ->
          text(
              player,
              tr("gui.input.schedule-time"),
              printable(world(action.value()).schedule().time()),
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old,
                              null,
                              null,
                              GuiConfigurationEditor.schedule(
                                  old, null, LocalTime.parse(value), null, null, null),
                              null,
                              null,
                              null)));
      case CYCLE_WEEKDAY ->
          editWorld(
              player,
              action.value(),
              old -> {
                DayOfWeek current =
                    Objects.requireNonNullElse(old.schedule().dayOfWeek(), DayOfWeek.MONDAY);
                DayOfWeek next = DayOfWeek.of(current.getValue() == 7 ? 1 : current.getValue() + 1);
                return GuiConfigurationEditor.copyWorld(
                    old,
                    null,
                    null,
                    GuiConfigurationEditor.schedule(old, null, null, next, null, null),
                    null,
                    null,
                    null);
              });
      case EDIT_MONTHDAY ->
          integer(
              player,
              tr("gui.input.month-day"),
              world(action.value()).schedule().dayOfMonth(),
              1,
              31,
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old,
                              null,
                              null,
                              GuiConfigurationEditor.schedule(old, null, null, null, value, null),
                              null,
                              null,
                              null)));
      case EDIT_INTERVAL ->
          integer(
              player,
              tr("gui.input.interval"),
              world(action.value()).schedule().intervalMinutes(),
              1,
              Integer.MAX_VALUE,
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old,
                              null,
                              null,
                              GuiConfigurationEditor.schedule(old, null, null, null, null, value),
                              null,
                              null,
                              null)));
      case EDIT_WARNINGS ->
          text(
              player,
              tr("gui.input.warnings"),
              join(world(action.value()).warnings()),
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old, null, null, null, parseWarnings(value), null, null)));
      case CYCLE_SEED ->
          editWorld(
              player,
              action.value(),
              old -> {
                SeedPolicy[] values = SeedPolicy.values();
                SeedPolicy next =
                    values[(old.regeneration().seedPolicy().ordinal() + 1) % values.length];
                RegenerationSettings regeneration =
                    new RegenerationSettings(
                        next,
                        next == SeedPolicy.FIXED
                            ? Objects.requireNonNullElse(old.regeneration().fixedSeed(), 0L)
                            : null,
                        old.regeneration().keepWorldConfig(),
                        old.regeneration().keepGameRules(),
                        old.regeneration().keepWorldBorder());
                return GuiConfigurationEditor.copyWorld(
                    old, null, null, null, null, regeneration, null);
              });
      case EDIT_FIXED_SEED ->
          longInput(
              player,
              tr("gui.input.fixed-seed"),
              world(action.value()).regeneration().fixedSeed(),
              value ->
                  editWorld(
                      player,
                      action.value(),
                      old ->
                          GuiConfigurationEditor.copyWorld(
                              old,
                              null,
                              null,
                              null,
                              null,
                              new RegenerationSettings(
                                  SeedPolicy.FIXED,
                                  value,
                                  old.regeneration().keepWorldConfig(),
                                  old.regeneration().keepGameRules(),
                                  old.regeneration().keepWorldBorder()),
                              null)));
      case TOGGLE_KEEP_CONFIG ->
          editRegeneration(
              player,
              action.value(),
              old ->
                  new RegenerationSettings(
                      old.seedPolicy(),
                      old.fixedSeed(),
                      !old.keepWorldConfig(),
                      old.keepGameRules(),
                      old.keepWorldBorder()));
      case TOGGLE_KEEP_RULES ->
          editRegeneration(
              player,
              action.value(),
              old ->
                  new RegenerationSettings(
                      old.seedPolicy(),
                      old.fixedSeed(),
                      old.keepWorldConfig(),
                      !old.keepGameRules(),
                      old.keepWorldBorder()));
      case TOGGLE_KEEP_BORDER ->
          editRegeneration(
              player,
              action.value(),
              old ->
                  new RegenerationSettings(
                      old.seedPolicy(),
                      old.fixedSeed(),
                      old.keepWorldConfig(),
                      old.keepGameRules(),
                      !old.keepWorldBorder()));
      case TOGGLE_EVACUATION ->
          editWorld(
              player,
              action.value(),
              old ->
                  GuiConfigurationEditor.copyWorld(
                      old,
                      null,
                      null,
                      null,
                      null,
                      null,
                      new EvacuationSettings(
                          !old.evacuation().enabled(), old.evacuation().destination())));
      case EDIT_EVACUATION ->
          text(
              player,
              tr("gui.input.evacuation"),
              printable(world(action.value()).evacuation().destination()),
              value -> {
                requireRegistered(value, tr("gui.field.evacuation"));
                if (value.equalsIgnoreCase(world(action.value()).multiverseWorld())) {
                  throw new IllegalArgumentException(tr("gui.error.evacuation-self"));
                }
                editWorld(
                    player,
                    action.value(),
                    old ->
                        GuiConfigurationEditor.copyWorld(
                            old,
                            null,
                            null,
                            null,
                            null,
                            null,
                            new EvacuationSettings(old.evacuation().enabled(), value)));
              });
      case CONFIRM_RESET -> openConfirmation(player, ConfirmKind.RESET, action.value());
      case CONFIRM_REMOVE -> openConfirmation(player, ConfirmKind.REMOVE_CONFIG, action.value());
      case EXECUTE_RESET -> executeReset(player, action.value());
      case EXECUTE_REMOVE -> {
        GuiEditResult edit = editor.removeWorld(action.value());
        localizedResult(player, edit, "gui.world-removed", () -> openDashboard(player, 0));
      }
      case EDIT_TIMEZONE ->
          text(
              player,
              tr("gui.input.timezone"),
              configs.current().timezone().getId(),
              value -> {
                try {
                  localizedResult(
                      player,
                      editor.setTimezone(ZoneId.of(value)),
                      "gui.timezone-saved",
                      () -> openGlobal(player));
                } catch (DateTimeException exception) {
                  throw new IllegalArgumentException(tr("gui.error.timezone"));
                }
              });
      case EDIT_HUB ->
          text(
              player,
              tr("gui.input.hub"),
              configs.current().defaultHubWorld(),
              value -> {
                requireRegistered(value, tr("gui.field.hub"));
                localizedResult(
                    player, editor.setHub(value), "gui.hub-saved", () -> openGlobal(player));
              });
      case EDIT_RETRIES ->
          integer(
              player,
              tr("gui.input.retries"),
              configs.current().resetPolicy().maxSafeRetries(),
              0,
              100,
              value ->
                  localizedResult(
                      player,
                      editor.updateResetPolicy(
                          old ->
                              new ResetPolicySettings(
                                  value, old.retryDelaySeconds(), old.broadcastCompletion())),
                      "gui.reset-policy-saved",
                      () -> openGlobal(player)));
      case EDIT_RETRY_DELAY ->
          integer(
              player,
              tr("gui.input.retry-delay"),
              configs.current().resetPolicy().retryDelaySeconds(),
              0,
              86400,
              value ->
                  localizedResult(
                      player,
                      editor.updateResetPolicy(
                          old ->
                              new ResetPolicySettings(
                                  old.maxSafeRetries(), value, old.broadcastCompletion())),
                      "gui.reset-policy-saved",
                      () -> openGlobal(player)));
      case TOGGLE_BROADCAST ->
          localizedResult(
              player,
              editor.updateResetPolicy(
                  old ->
                      new ResetPolicySettings(
                          old.maxSafeRetries(),
                          old.retryDelaySeconds(),
                          !old.broadcastCompletion())),
              "gui.reset-policy-saved",
              () -> openGlobal(player));
      case TOGGLE_DISCOVERY ->
          updateTeleport(
              player,
              old ->
                  new TeleportSettings(
                      !old.autoDiscover(), old.defaultEnabled(), old.showLocked(), old.worlds()));
      case TOGGLE_DEFAULT_TELEPORT ->
          updateTeleport(
              player,
              old ->
                  new TeleportSettings(
                      old.autoDiscover(), !old.defaultEnabled(), old.showLocked(), old.worlds()));
      case TOGGLE_SHOW_LOCKED ->
          updateTeleport(
              player,
              old ->
                  new TeleportSettings(
                      old.autoDiscover(), old.defaultEnabled(), !old.showLocked(), old.worlds()));
      case TOGGLE_TELEPORT_WORLD ->
          updateTeleportWorld(
              player,
              action.value(),
              old -> new TeleportDestinationSettings(!old.enabled(), old.permission()));
      case EDIT_PERMISSION ->
          text(
              player,
              tr("gui.input.permission"),
              printable(teleportWorld(action.value()).permission()),
              value ->
                  updateTeleportWorld(
                      player,
                      action.value(),
                      old -> new TeleportDestinationSettings(old.enabled(), nullable(value))));
      case REMOVE_TELEPORT_OVERRIDE ->
          localizedResult(
              player,
              editor.removeTeleportOverride(action.value()),
              "gui.teleport-override-removed",
              () -> openTeleportWorld(player, action.value()));
      case CLOSE -> player.closeInventory();
      default -> throw new AssertionError("Unhandled GUI action: " + action.type());
    }
  }

  private void openDashboard(Player player, int requestedPage) {
    PluginSettings settings = configs.current();
    List<ManagedWorldSettings> worlds = List.copyOf(settings.worlds().values());
    int page = boundedPage(requestedPage, worlds.size());
    DashboardHolder holder = new DashboardHolder(page);
    final Inventory inventory =
        inventory(holder, 54, messages.component("gui.admin.dashboard-title"));
    holder.add(
        4,
        item(
            Material.MAP,
            "&6" + tr("gui.admin.managed-worlds"),
            tr("gui.common.page-of", "page", page + 1, "pages", pageCount(worlds.size())),
            tr("gui.admin.configured-count", "count", worlds.size()),
            tr("gui.admin.entries-order")),
        null);
    List<ManagedWorldSettings> visible = page(worlds, page);
    for (int index = 0; index < visible.size(); index++) {
      ManagedWorldSettings world = visible.get(index);
      holder.add(
          CONTENT_SLOTS[index],
          item(
              world.canReset() ? Material.GRASS_BLOCK : Material.BARRIER,
              color(world.canReset()) + world.displayName(),
              tr("gui.common.id", "id", world.id()),
              tr("gui.common.world-key", "world", world.multiverseWorld()),
              tr("gui.common.state", "state", operationalState(world)),
              tr(
                  "gui.common.next",
                  "next",
                  schedules
                      .nextRun(world.id())
                      .map(Object::toString)
                      .orElseGet(() -> tr("value.not-scheduled"))),
              tr("gui.admin.click-configure")),
          new GuiAction(ActionType.WORLD, world.id(), 0));
    }
    holder.add(
        45,
        item(
            Material.EMERALD,
            "&a" + tr("gui.admin.add-world"),
            tr("gui.admin.add-world-description")),
        new GuiAction(ActionType.ADD_WORLD, "", 0));
    holder.add(
        46,
        item(Material.COMPARATOR, "&e" + tr("gui.admin.global-settings")),
        action(ActionType.GLOBAL));
    holder.add(
        47,
        item(Material.ENDER_PEARL, "&b" + tr("gui.admin.teleport-admin")),
        action(ActionType.TELEPORTS));
    holder.add(
        48, item(Material.BOOK, "&f" + tr("gui.admin.reset-history")), action(ActionType.HISTORY));
    holder.add(
        49,
        item(Material.CLOCK, "&e" + tr("gui.admin.reload"), tr("gui.admin.reload-description")),
        action(ActionType.RELOAD));
    navigation(holder, 52, 53, page, worlds.size(), ActionType.DASHBOARD);
    holder.add(50, item(Material.BARRIER, "&c" + tr("gui.common.close")), action(ActionType.CLOSE));
    player.openInventory(inventory);
  }

  private void openAddWorld(Player player, int requestedPage) {
    PluginSettings settings = configs.current();
    Set<String> configured = new LinkedHashSet<>();
    settings
        .worlds()
        .values()
        .forEach(world -> configured.add(world.multiverseWorld().toLowerCase(Locale.ROOT)));
    List<WorldSnapshot> worlds = List.copyOf(gateway.registeredWorlds());
    int page = boundedPage(requestedPage, worlds.size());
    AddWorldHolder holder = new AddWorldHolder(page);
    final Inventory inventory = inventory(holder, 54, messages.component("gui.admin.add-title"));
    List<WorldSnapshot> visible = page(worlds, page);
    for (int index = 0; index < visible.size(); index++) {
      WorldSnapshot world = visible.get(index);
      boolean protectedWorld =
          world.name().equalsIgnoreCase(settings.defaultHubWorld())
              || world.name().equalsIgnoreCase(gateway.defaultWorldName());
      boolean exists = configured.contains(world.name().toLowerCase(Locale.ROOT));
      boolean addable = !protectedWorld && !exists;
      List<String> lore =
          List.of(
              tr(
                  "gui.common.status",
                  "status",
                  tr(world.loaded() ? "value.loaded" : "value.unloaded")),
              tr(
                  "gui.admin.rwr-status",
                  "status",
                  tr(
                      exists
                          ? "value.already-configured"
                          : protectedWorld ? "value.protected" : "value.available")),
              tr(addable ? "gui.admin.click-add" : "gui.admin.cannot-select"));
      int slot = CONTENT_SLOTS[index];
      holder.add(
          slot,
          item(
              addable ? Material.GRASS_BLOCK : Material.BARRIER,
              (addable ? "&a" : "&c") + world.name(),
              lore.toArray(String[]::new)),
          addable ? new GuiAction(ActionType.ADD_SELECTED, world.name(), 0) : null);
    }
    holder.add(
        49, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.DASHBOARD));
    navigation(holder, 52, 53, page, worlds.size(), ActionType.ADD_WORLD);
    player.openInventory(inventory);
  }

  private void openWorld(Player player, String id) {
    ManagedWorldSettings world = configs.current().world(id).orElse(null);
    if (world == null) {
      message(player, false, tr("gui.error.world-missing"));
      openDashboard(player, 0);
      return;
    }
    WorldHolder holder = new WorldHolder(id);
    final Inventory inv =
        inventory(
            holder,
            54,
            messages.component(
                "gui.admin.world-title", "world", truncate(world.displayName(), 22)));
    holder.add(
        0,
        item(Material.NAME_TAG, "&e" + tr("gui.world.display-name"), world.displayName()),
        value(ActionType.EDIT_DISPLAY, id));
    holder.add(
        1,
        toggle(
            tr("gui.world.scheduling"),
            world.enabled(),
            tr("gui.common.state", "state", operationalState(world))),
        value(ActionType.TOGGLE_ENABLED, id));
    holder.add(
        2,
        item(
            Material.REPEATER,
            "&e" + tr("gui.world.schedule-type"),
            tr("value.schedule." + world.schedule().type().name().toLowerCase(Locale.ROOT))),
        value(ActionType.CYCLE_SCHEDULE, id));
    holder.add(
        3,
        item(
            Material.CLOCK,
            "&e" + tr("gui.world.time"),
            printable(world.schedule().time()),
            tr("gui.world.time-description")),
        value(ActionType.EDIT_TIME, id));
    if (world.schedule().type() == ScheduleType.WEEKLY) {
      holder.add(
          4,
          item(
              Material.PAPER,
              "&e" + tr("gui.world.weekday"),
              tr("value.weekday." + world.schedule().dayOfWeek().name().toLowerCase(Locale.ROOT))),
          value(ActionType.CYCLE_WEEKDAY, id));
    } else if (world.schedule().type() == ScheduleType.MONTHLY) {
      holder.add(
          4,
          item(
              Material.PAPER,
              "&e" + tr("gui.world.month-day"),
              Integer.toString(world.schedule().dayOfMonth())),
          value(ActionType.EDIT_MONTHDAY, id));
    } else if (world.schedule().type() == ScheduleType.INTERVAL) {
      holder.add(
          4,
          item(
              Material.PAPER,
              "&e" + tr("gui.world.interval"),
              Integer.toString(world.schedule().intervalMinutes())),
          value(ActionType.EDIT_INTERVAL, id));
    }
    holder.add(
        5,
        item(
            Material.BELL,
            "&e" + tr("gui.world.warnings"),
            join(world.warnings()),
            tr("gui.world.warnings-description"),
            tr("gui.world.warnings-example"),
            tr("gui.world.warnings-disable")),
        value(ActionType.EDIT_WARNINGS, id));
    holder.add(
        9,
        item(
            Material.WHEAT_SEEDS,
            "&e" + tr("gui.world.seed-policy"),
            tr("value.seed." + world.regeneration().seedPolicy().name().toLowerCase(Locale.ROOT))),
        value(ActionType.CYCLE_SEED, id));
    if (world.regeneration().seedPolicy() == SeedPolicy.FIXED) {
      holder.add(
          10,
          item(
              Material.PAPER,
              "&e" + tr("gui.world.fixed-seed"),
              printable(world.regeneration().fixedSeed())),
          value(ActionType.EDIT_FIXED_SEED, id));
    }
    holder.add(
        11,
        toggle(tr("gui.world.keep-config"), world.regeneration().keepWorldConfig()),
        value(ActionType.TOGGLE_KEEP_CONFIG, id));
    holder.add(
        12,
        toggle(tr("gui.world.keep-gamerules"), world.regeneration().keepGameRules()),
        value(ActionType.TOGGLE_KEEP_RULES, id));
    holder.add(
        13,
        toggle(tr("gui.world.keep-border"), world.regeneration().keepWorldBorder()),
        value(ActionType.TOGGLE_KEEP_BORDER, id));
    holder.add(
        18,
        toggle(tr("gui.world.evacuate"), world.evacuation().enabled()),
        value(ActionType.TOGGLE_EVACUATION, id));
    holder.add(
        19,
        item(
            Material.COMPASS,
            "&e" + tr("gui.world.evacuation-destination"),
            printable(world.evacuation().destination())),
        value(ActionType.EDIT_EVACUATION, id));
    ResetPolicySettings policy = configs.current().resetPolicy();
    holder.add(
        20,
        item(
            Material.SHIELD,
            "&e" + tr("gui.world.failure-policy"),
            tr("gui.world.safe-retries", "count", policy.maxSafeRetries()),
            tr("gui.world.retry-delay", "seconds", policy.retryDelaySeconds()),
            tr("gui.world.edit-global")),
        action(ActionType.GLOBAL));
    holder.add(
        45,
        item(
            world.canReset() ? Material.TNT : Material.BARRIER,
            "&c" + tr("gui.world.reset-now"),
            world.canReset()
                ? tr("gui.common.requires-confirmation")
                : tr("gui.world.blocked", "state", operationalState(world))),
        world.canReset() ? value(ActionType.CONFIRM_RESET, id) : null);
    holder.add(
        46,
        item(
            Material.LAVA_BUCKET,
            "&c" + tr("gui.world.remove"),
            tr("gui.world.remove-description"),
            tr("gui.common.requires-confirmation")),
        value(ActionType.CONFIRM_REMOVE, id));
    holder.add(
        49, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.DASHBOARD));
    player.openInventory(inv);
  }

  private void openGlobal(Player player) {
    PluginSettings settings = configs.current();
    GlobalHolder holder = new GlobalHolder();
    final Inventory inv = inventory(holder, 27, messages.component("gui.global.title"));
    holder.add(
        0,
        item(
            Material.CLOCK,
            "&e" + tr("gui.global.timezone"),
            settings.timezone().getId(),
            tr("gui.global.timezone-description")),
        action(ActionType.EDIT_TIMEZONE));
    holder.add(
        1,
        item(
            Material.RECOVERY_COMPASS,
            "&e" + tr("gui.global.hub"),
            settings.defaultHubWorld(),
            tr("gui.global.hub-description")),
        action(ActionType.EDIT_HUB));
    holder.add(
        3,
        item(
            Material.SHIELD,
            "&e" + tr("gui.global.retries"),
            Integer.toString(settings.resetPolicy().maxSafeRetries())),
        action(ActionType.EDIT_RETRIES));
    holder.add(
        4,
        item(
            Material.CLOCK,
            "&e" + tr("gui.global.retry-delay"),
            Integer.toString(settings.resetPolicy().retryDelaySeconds())),
        action(ActionType.EDIT_RETRY_DELAY));
    holder.add(
        5,
        toggle(tr("gui.global.broadcast"), settings.resetPolicy().broadcastCompletion()),
        action(ActionType.TOGGLE_BROADCAST));
    holder.add(
        9,
        toggle(tr("gui.global.auto-discovery"), settings.teleport().autoDiscover()),
        action(ActionType.TOGGLE_DISCOVERY));
    holder.add(
        10,
        toggle(tr("gui.global.discovered-enabled"), settings.teleport().defaultEnabled()),
        action(ActionType.TOGGLE_DEFAULT_TELEPORT));
    holder.add(
        11,
        toggle(tr("gui.global.show-locked"), settings.teleport().showLocked()),
        action(ActionType.TOGGLE_SHOW_LOCKED));
    holder.add(
        22, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.DASHBOARD));
    player.openInventory(inv);
  }

  private void openTeleports(Player player, int requestedPage) {
    TeleportSettings settings = configs.current().teleport();
    Set<String> names = new LinkedHashSet<>();
    gateway.registeredWorlds().forEach(world -> names.add(world.name()));
    names.addAll(settings.worlds().keySet());
    List<String> worlds = List.copyOf(names);
    int page = boundedPage(requestedPage, worlds.size());
    TeleportHolder holder = new TeleportHolder(page);
    final Inventory inv = inventory(holder, 54, messages.component("gui.teleport-admin.title"));
    List<String> visible = page(worlds, page);
    for (int index = 0; index < visible.size(); index++) {
      String name = visible.get(index);
      TeleportDestinationSettings destination =
          settings
              .worlds()
              .getOrDefault(name, new TeleportDestinationSettings(settings.defaultEnabled(), null));
      boolean loaded = gateway.world(name).map(WorldSnapshot::loaded).orElse(false);
      holder.add(
          CONTENT_SLOTS[index],
          item(
              destination.enabled() ? Material.ENDER_PEARL : Material.ENDER_EYE,
              (destination.enabled() ? "&a" : "&c") + displayName(name),
              tr("gui.teleport.world", "world", name),
              tr(
                  "gui.common.status",
                  "status",
                  tr(loaded ? "value.loaded" : "value.unloaded-or-orphaned")),
              tr("gui.teleport.permission", "permission", printable(destination.permission())),
              tr(
                  settings.worlds().containsKey(name)
                      ? "gui.teleport-admin.explicit"
                      : "gui.teleport-admin.discovery")),
          value(ActionType.TELEPORT_WORLD, name));
    }
    holder.add(
        45,
        toggle(tr("gui.teleport-admin.auto-discover"), settings.autoDiscover()),
        action(ActionType.TOGGLE_DISCOVERY));
    holder.add(
        46,
        toggle(tr("gui.teleport-admin.default-enabled"), settings.defaultEnabled()),
        action(ActionType.TOGGLE_DEFAULT_TELEPORT));
    holder.add(
        47,
        toggle(tr("gui.teleport-admin.show-locked"), settings.showLocked()),
        action(ActionType.TOGGLE_SHOW_LOCKED));
    holder.add(
        49, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.DASHBOARD));
    navigation(holder, 52, 53, page, worlds.size(), ActionType.TELEPORTS);
    player.openInventory(inv);
  }

  private void openTeleportWorld(Player player, String worldName) {
    TeleportDestinationSettings destination = teleportWorld(worldName);
    TeleportWorldHolder holder = new TeleportWorldHolder(worldName);
    final Inventory inv =
        inventory(
            holder,
            27,
            messages.component("gui.teleport-admin.world-title", "world", truncate(worldName, 18)));
    holder.add(
        0,
        toggle(tr("gui.teleport-admin.destination-enabled"), destination.enabled()),
        value(ActionType.TOGGLE_TELEPORT_WORLD, worldName));
    holder.add(
        1,
        item(
            Material.NAME_TAG,
            "&e" + tr("gui.teleport-admin.shared-name"),
            displayName(worldName),
            tr("gui.teleport-admin.shared-name-description"),
            tr("gui.common.world-key", "world", worldName)),
        null);
    holder.add(
        2,
        item(
            Material.TRIPWIRE_HOOK,
            "&e" + tr("gui.teleport-admin.permission"),
            printable(destination.permission()),
            tr("gui.teleport-admin.permission-clear")),
        value(ActionType.EDIT_PERMISSION, worldName));
    holder.add(
        9,
        item(
            Material.BUCKET,
            "&e" + tr("gui.teleport-admin.use-defaults"),
            tr("gui.teleport-admin.use-defaults-description")),
        value(ActionType.REMOVE_TELEPORT_OVERRIDE, worldName));
    if (gateway.world(worldName).isEmpty()) {
      holder.add(
          4,
          item(
              Material.BARRIER,
              "&c" + tr("gui.teleport-admin.orphaned"),
              tr("gui.teleport-admin.orphaned-description"),
              tr("gui.teleport-admin.orphaned-action")),
          null);
    }
    holder.add(
        22, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.TELEPORTS));
    player.openInventory(inv);
  }

  private void openHistory(Player player) {
    List<ResetHistoryEntry> entries = new ArrayList<>(resets.recentHistory(45));
    Collections.reverse(entries);
    HistoryHolder holder = new HistoryHolder();
    Inventory inv = inventory(holder, 54, messages.component("gui.history.title"));
    for (ResetHistoryEntry entry : entries) {
      int slot = inv.firstEmpty();
      if (slot < 0 || slot >= 45) {
        break;
      }
      holder.add(
          slot,
          item(
              entry.failure() == null ? Material.LIME_DYE : Material.RED_DYE,
              (entry.failure() == null ? "&a" : "&c")
                  + WorldDisplayNames.resolveId(configs.current(), entry.worldId()),
              tr("gui.history.completed", "time", formatTimestamp(entry.completedAt())),
              tr(
                  "gui.history.result",
                  "result",
                  tr("value.phase." + entry.terminalPhase().name().toLowerCase(Locale.ROOT))),
              tr("gui.history.failure", "failure", printable(entry.failure())),
              entry.failure() == null
                  ? tr("value.successful")
                  : tr("value.failure." + entry.failure().name().toLowerCase(Locale.ROOT))),
          null);
    }
    holder.add(
        49, item(Material.ARROW, "&e" + tr("gui.common.back")), action(ActionType.DASHBOARD));
    player.openInventory(inv);
  }

  private void openConfirmation(Player player, ConfirmKind kind, String id) {
    ManagedWorldSettings world = configs.current().world(id).orElse(null);
    if (world == null || (kind == ConfirmKind.RESET && !world.canReset())) {
      message(player, false, tr("gui.error.action-unavailable"));
      openDashboard(player, 0);
      return;
    }
    ConfirmationHolder holder = new ConfirmationHolder(kind, id);
    Inventory inv =
        inventory(
            holder,
            27,
            messages.component(
                kind == ConfirmKind.RESET
                    ? "gui.confirm.reset-title"
                    : "gui.confirm.remove-title"));
    String[] lore =
        kind == ConfirmKind.RESET
            ? new String[] {tr("gui.confirm.reset-description"), tr("gui.confirm.reset-protected")}
            : new String[] {
              tr("gui.confirm.remove-description"),
              tr("gui.confirm.remove-keeps-world"),
              tr("gui.confirm.remove-no-delete")
            };
    holder.add(
        11,
        item(
            Material.RED_CONCRETE,
            "&c"
                + tr(
                    kind == ConfirmKind.RESET
                        ? "gui.confirm.confirm-reset"
                        : "gui.confirm.confirm-remove"),
            lore),
        value(
            kind == ConfirmKind.RESET ? ActionType.EXECUTE_RESET : ActionType.EXECUTE_REMOVE, id));
    holder.add(
        15,
        item(Material.LIME_CONCRETE, "&a" + tr("gui.common.cancel")),
        value(ActionType.WORLD, id));
    player.openInventory(inv);
  }

  private void executeReset(Player player, String id) {
    ManagedWorldSettings world = configs.current().world(id).orElse(null);
    if (world == null || !world.canReset()) {
      message(player, false, tr("gui.error.reset-blocked"));
      openDashboard(player, 0);
      return;
    }
    player.closeInventory();
    messages.send(
        player, "command.reset-start", "world", world.displayName(), "world_id", world.id());
    plugin
        .getServer()
        .getGlobalRegionScheduler()
        .run(
            plugin,
            ignored ->
                schedules
                    .resetNowAsync(id, false)
                    .thenAccept(
                        outcome ->
                            player
                                .getScheduler()
                                .run(
                                    plugin,
                                    task -> {
                                      if (outcome.successful()) {
                                        messages.send(
                                            player,
                                            "notification.reset-complete",
                                            "world",
                                            world.displayName());
                                      } else {
                                        messages.send(
                                            player,
                                            "notification.reset-failed",
                                            "world",
                                            world.displayName(),
                                            "failure",
                                            outcome.failure());
                                      }
                                    },
                                    null)));
  }

  private void reload(Player player) {
    var result = configs.reload();
    if (result.accepted()) {
      updates.reloadSettings();
      if (messages.reload()) {
        messages.send(player, "gui.reload-success");
      } else {
        messages.send(player, "command.locale-reload-failed");
      }
    } else {
      messages.send(player, "gui.reload-failed");
      result.issues().forEach(issue -> messages.send(player, "gui.issue", "issue", issue));
    }
    openDashboard(player, 0);
  }

  private void editWorld(
      Player player, String id, java.util.function.UnaryOperator<ManagedWorldSettings> edit) {
    localizedResult(
        player,
        editor.updateWorld(id, edit, tr("gui.world-saved-plain")),
        "gui.world-saved",
        () -> openWorld(player, id));
  }

  private void editRegeneration(
      Player player, String id, java.util.function.UnaryOperator<RegenerationSettings> edit) {
    editWorld(
        player,
        id,
        old ->
            GuiConfigurationEditor.copyWorld(
                old, null, null, null, null, edit.apply(old.regeneration()), null));
  }

  private void updateTeleport(
      Player player, java.util.function.UnaryOperator<TeleportSettings> edit) {
    result(
        player,
        editor.updateTeleport(edit, tr("gui.teleport-defaults-saved")),
        () -> openTeleports(player, 0));
  }

  private void updateTeleportWorld(
      Player player,
      String name,
      java.util.function.UnaryOperator<TeleportDestinationSettings> edit) {
    result(
        player,
        editor.updateTeleportWorld(name, edit, tr("gui.teleport-destination-saved")),
        () -> openTeleportWorld(player, name));
  }

  private void text(Player player, String title, String initial, Consumer<String> accepted) {
    input.request(
        player,
        title,
        initial,
        value -> {
          try {
            accepted.accept(value);
          } catch (NumberFormatException exception) {
            message(player, false, tr("gui.error.whole-number"));
          } catch (DateTimeParseException exception) {
            message(player, false, tr("gui.error.time-format"));
          } catch (IllegalArgumentException exception) {
            message(player, false, exception.getMessage());
          }
        });
  }

  private void integer(
      Player player,
      String title,
      int initial,
      int minimum,
      int maximum,
      Consumer<Integer> accepted) {
    text(
        player,
        title,
        Integer.toString(initial),
        value -> {
          int parsed = Integer.parseInt(value);
          if (parsed < minimum || parsed > maximum) {
            throw new IllegalArgumentException(
                tr("gui.error.range", "minimum", minimum, "maximum", maximum));
          }
          accepted.accept(parsed);
        });
  }

  private void longInput(Player player, String title, Long initial, Consumer<Long> accepted) {
    text(player, title, printable(initial), value -> accepted.accept(Long.parseLong(value)));
  }

  private void result(Player player, GuiEditResult edit, Runnable reopen) {
    if (edit.accepted()) {
      message(player, true, edit.message());
    } else {
      messages.send(player, "gui.edit-rejected");
    }
    edit.issues().forEach(issue -> messages.send(player, "gui.issue", "issue", issue));
    reopen.run();
  }

  private void localizedResult(
      Player player, GuiEditResult edit, String successKey, Runnable reopen) {
    if (edit.accepted()) {
      messages.send(player, successKey);
    } else {
      messages.send(player, "gui.edit-rejected");
    }
    edit.issues().forEach(issue -> messages.send(player, "gui.issue", "issue", issue));
    reopen.run();
  }

  private ManagedWorldSettings world(String id) {
    return configs
        .current()
        .world(id)
        .orElseThrow(() -> new IllegalArgumentException(tr("gui.error.world-changed")));
  }

  private TeleportDestinationSettings teleportWorld(String name) {
    TeleportSettings teleport = configs.current().teleport();
    return teleport
        .worlds()
        .getOrDefault(name, new TeleportDestinationSettings(teleport.defaultEnabled(), null));
  }

  private String displayName(String multiverseWorld) {
    return WorldDisplayNames.resolve(configs.current(), multiverseWorld);
  }

  private void openWorldByName(Player player, String name) {
    configs.current().worlds().values().stream()
        .filter(world -> world.multiverseWorld().equalsIgnoreCase(name))
        .findFirst()
        .ifPresentOrElse(world -> openWorld(player, world.id()), () -> openAddWorld(player, 0));
  }

  private void requireRegistered(String name, String field) {
    if (gateway.world(name).isEmpty()) {
      throw new IllegalArgumentException(tr("gui.error.registered-world", "field", field));
    }
  }

  private static ScheduleSettings defaultsFor(ScheduleType type, ScheduleSettings old) {
    LocalTime time = Objects.requireNonNullElse(old.time(), LocalTime.of(3, 0));
    return switch (type) {
      case DAILY -> new ScheduleSettings(type, time, null, 0, 0);
      case WEEKLY -> new ScheduleSettings(type, time, DayOfWeek.MONDAY, 0, 0);
      case MONTHLY -> new ScheduleSettings(type, time, null, 1, 0);
      case INTERVAL ->
          new ScheduleSettings(type, null, null, 0, Math.max(1, old.intervalMinutes()));
    };
  }

  private String required(String value, String field) {
    if (value.isBlank()) {
      throw new IllegalArgumentException(tr("gui.error.blank", "field", field));
    }
    return value;
  }

  private String nullable(String value) {
    return value.isBlank()
            || value.equalsIgnoreCase("none")
            || value.equalsIgnoreCase(tr("value.none"))
        ? null
        : value;
  }

  private List<Integer> parseWarnings(String value) {
    String normalized = value.equalsIgnoreCase(tr("value.none")) ? "none" : value;
    try {
      return GuiConfigurationEditor.parseWarnings(normalized);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(tr("gui.error.warnings"), exception);
    }
  }

  private static Inventory inventory(AdminHolder holder, int size, Component title) {
    Inventory inventory = Bukkit.createInventory(holder, size, title);
    holder.attach(inventory);
    return inventory;
  }

  private static ItemStack item(Material material, String name, String... lore) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.displayName(LEGACY_TEXT.deserialize(name));
    List<Component> colored = new ArrayList<>();
    for (String line : lore) {
      colored.add(LEGACY_TEXT.deserialize("&7" + line));
    }
    meta.lore(colored);
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    item.setItemMeta(meta);
    return item;
  }

  private ItemStack toggle(String name, boolean enabled, String... extra) {
    List<String> lore = new ArrayList<>();
    lore.add(tr(enabled ? "value.enabled" : "value.disabled"));
    lore.addAll(List.of(extra));
    return item(
        enabled ? Material.LIME_DYE : Material.GRAY_DYE,
        (enabled ? "&a" : "&c") + name,
        lore.toArray(String[]::new));
  }

  private void navigation(
      AdminHolder holder,
      int previousSlot,
      int nextSlot,
      int page,
      int itemCount,
      ActionType type) {
    if (page > 0) {
      holder.add(
          previousSlot,
          item(Material.ARROW, "&e" + tr("gui.common.previous-page")),
          new GuiAction(type, "", page - 1));
    }
    if ((page + 1) * PAGE_SIZE < itemCount) {
      holder.add(
          nextSlot,
          item(Material.ARROW, "&e" + tr("gui.common.next-page")),
          new GuiAction(type, "", page + 1));
    }
  }

  private static <T> List<T> page(List<T> values, int page) {
    int start = page * PAGE_SIZE;
    return values.subList(
        Math.min(start, values.size()), Math.min(start + PAGE_SIZE, values.size()));
  }

  private static int boundedPage(int requested, int size) {
    int last = Math.max(0, (size - 1) / PAGE_SIZE);
    return Math.max(0, Math.min(requested, last));
  }

  private static int pageCount(int size) {
    return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
  }

  private static GuiAction action(ActionType type) {
    return new GuiAction(type, "", 0);
  }

  private static GuiAction value(ActionType type, String value) {
    return new GuiAction(type, value, 0);
  }

  private String join(List<Integer> values) {
    return values.isEmpty()
        ? tr("value.none")
        : values.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElseGet(() -> tr("value.none"));
  }

  private String printable(Object value) {
    return value == null ? tr("value.none") : value.toString().replace("MULTIVERSE", "WORLDS");
  }

  private static String paperTerminology(String message) {
    return message == null
        ? ""
        : message.replace("Multiverse-Core", "Worlds").replace("Multiverse", "Worlds");
  }

  private static String truncate(String value, int maximum) {
    return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
  }

  private static String color(boolean positive) {
    return positive ? "&a" : "&c";
  }

  private String operationalState(ManagedWorldSettings world) {
    return tr("value.state." + world.state().name().toLowerCase(Locale.ROOT));
  }

  private String formatTimestamp(String timestamp) {
    try {
      return HISTORY_TIME.format(Instant.parse(timestamp).atZone(configs.current().timezone()));
    } catch (RuntimeException exception) {
      return timestamp;
    }
  }

  private String tr(String key, Object... placeholders) {
    return messages.plain(key, placeholders);
  }

  private void message(Player player, boolean success, String message) {
    messages.send(player, success ? "gui.success" : "gui.failure", "message", message);
  }

  private enum ConfirmKind {
    RESET,
    REMOVE_CONFIG
  }

  private enum ActionType {
    DASHBOARD,
    ADD_WORLD,
    WORLD,
    ADD_SELECTED,
    GLOBAL,
    TELEPORTS,
    TELEPORT_WORLD,
    HISTORY,
    RELOAD,
    EDIT_DISPLAY,
    TOGGLE_ENABLED,
    CYCLE_SCHEDULE,
    EDIT_TIME,
    CYCLE_WEEKDAY,
    EDIT_MONTHDAY,
    EDIT_INTERVAL,
    EDIT_WARNINGS,
    CYCLE_SEED,
    EDIT_FIXED_SEED,
    TOGGLE_KEEP_CONFIG,
    TOGGLE_KEEP_RULES,
    TOGGLE_KEEP_BORDER,
    TOGGLE_EVACUATION,
    EDIT_EVACUATION,
    CONFIRM_RESET,
    CONFIRM_REMOVE,
    EXECUTE_RESET,
    EXECUTE_REMOVE,
    EDIT_TIMEZONE,
    EDIT_HUB,
    EDIT_RETRIES,
    EDIT_RETRY_DELAY,
    TOGGLE_BROADCAST,
    TOGGLE_DISCOVERY,
    TOGGLE_DEFAULT_TELEPORT,
    TOGGLE_SHOW_LOCKED,
    TOGGLE_TELEPORT_WORLD,
    EDIT_PERMISSION,
    REMOVE_TELEPORT_OVERRIDE,
    CLOSE
  }

  private record GuiAction(ActionType type, String value, int number) {}

  private abstract static class AdminHolder implements InventoryHolder {
    private final java.util.Map<Integer, GuiAction> actions = new java.util.HashMap<>();
    private Inventory inventory;

    final void attach(Inventory value) {
      inventory = value;
    }

    final void add(int slot, ItemStack item, GuiAction action) {
      if (slot < 0 || item == null) {
        return;
      }
      inventory.setItem(slot, item);
      if (action != null) {
        actions.put(slot, action);
      }
    }

    final GuiAction action(int slot) {
      return actions.get(slot);
    }

    @Override
    public final Inventory getInventory() {
      return inventory;
    }
  }

  private static final class DashboardHolder extends AdminHolder {
    DashboardHolder(int page) {}
  }

  private static final class AddWorldHolder extends AdminHolder {
    AddWorldHolder(int page) {}
  }

  private static final class WorldHolder extends AdminHolder {
    WorldHolder(String id) {}
  }

  private static final class GlobalHolder extends AdminHolder {}

  private static final class TeleportHolder extends AdminHolder {
    TeleportHolder(int page) {}
  }

  private static final class TeleportWorldHolder extends AdminHolder {
    TeleportWorldHolder(String name) {}
  }

  private static final class HistoryHolder extends AdminHolder {}

  private static final class ConfirmationHolder extends AdminHolder {
    ConfirmationHolder(ConfirmKind kind, String id) {}
  }
}
