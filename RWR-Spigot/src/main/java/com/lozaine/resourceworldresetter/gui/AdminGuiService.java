package com.lozaine.resourceworldresetter.gui;

import com.lozaine.resourceworldresetter.config.EvacuationSettings;
import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.config.PluginSettings;
import com.lozaine.resourceworldresetter.config.RegenerationSettings;
import com.lozaine.resourceworldresetter.config.ResetPolicySettings;
import com.lozaine.resourceworldresetter.config.ScheduleSettings;
import com.lozaine.resourceworldresetter.config.ScheduleType;
import com.lozaine.resourceworldresetter.config.TeleportDestinationSettings;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.config.WorldDisplayNames;
import com.lozaine.resourceworldresetter.history.ResetHistoryEntry;
import com.lozaine.resourceworldresetter.message.MessageService;
import com.lozaine.resourceworldresetter.multiverse.SeedPolicy;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import com.lozaine.resourceworldresetter.reset.ResetCoordinator;
import com.lozaine.resourceworldresetter.reset.ResetOutcome;
import com.lozaine.resourceworldresetter.scheduler.ScheduleManager;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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
import org.bukkit.plugin.java.JavaPlugin;

/** All M5 admin screens and the single action router that owns them. */
public final class AdminGuiService implements Listener {
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_SIZE = CONTENT_SLOTS.length;
    private final JavaPlugin plugin;
    private final com.lozaine.resourceworldresetter.config.ConfigService configs;
    private final WorldProvider gateway;
    private final ResetCoordinator resets;
    private final ScheduleManager schedules;
    private final GuiConfigurationEditor editor;
    private final GuiInputService input;
    private final MessageService messages;

    public AdminGuiService(
            JavaPlugin plugin,
            com.lozaine.resourceworldresetter.config.ConfigService configs,
            WorldProvider gateway,
            ResetCoordinator resets,
            ScheduleManager schedules,
            GuiInputService input,
            MessageService messages) {
        this.plugin = plugin;
        this.configs = configs;
        this.gateway = gateway;
        this.resets = resets;
        this.schedules = schedules;
        this.editor = new GuiConfigurationEditor(configs, gateway);
        this.input = input;
        this.messages = messages;
    }

    public void open(Player player) {
        openDashboard(player, 0);
    }

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
            case ADD_SELECTED -> result(player, editor.addWorld(action.value()), () -> openWorldByName(player, action.value()));
            case GLOBAL -> openGlobal(player);
            case TELEPORTS -> openTeleports(player, action.number());
            case TELEPORT_WORLD -> openTeleportWorld(player, action.value());
            case HISTORY -> openHistory(player);
            case RELOAD -> reload(player);
            case EDIT_DISPLAY -> text(player, "World display name", world(action.value()).displayName(), value ->
                    editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                            old, required(value, "Display name"), null, null, null, null, null)));
            case TOGGLE_ENABLED -> editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                    old, null, !old.enabled(), null, null, null, null));
            case CYCLE_SCHEDULE -> editWorld(player, action.value(), old -> {
                ScheduleType[] values = ScheduleType.values();
                ScheduleType next = values[(old.schedule().type().ordinal() + 1) % values.length];
                ScheduleSettings schedule = defaultsFor(next, old.schedule());
                return GuiConfigurationEditor.copyWorld(old, null, null, schedule, null, null, null);
            });
            case EDIT_TIME -> text(player, "Schedule time (HH:mm)", printable(world(action.value()).schedule().time()), value ->
                    editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(old, null, null,
                            GuiConfigurationEditor.schedule(old, null, LocalTime.parse(value), null, null, null),
                            null, null, null)));
            case CYCLE_WEEKDAY -> editWorld(player, action.value(), old -> {
                DayOfWeek current = Objects.requireNonNullElse(old.schedule().dayOfWeek(), DayOfWeek.MONDAY);
                DayOfWeek next = DayOfWeek.of(current.getValue() == 7 ? 1 : current.getValue() + 1);
                return GuiConfigurationEditor.copyWorld(old, null, null,
                        GuiConfigurationEditor.schedule(old, null, null, next, null, null), null, null, null);
            });
            case EDIT_MONTHDAY -> integer(player, "Day of month (1-31)", world(action.value()).schedule().dayOfMonth(),
                    1, 31, value -> editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                            old, null, null, GuiConfigurationEditor.schedule(old, null, null, null, value, null),
                            null, null, null)));
            case EDIT_INTERVAL -> integer(player, "Interval minutes", world(action.value()).schedule().intervalMinutes(),
                    1, Integer.MAX_VALUE, value -> editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                            old, null, null, GuiConfigurationEditor.schedule(old, null, null, null, null, value),
                            null, null, null)));
            case EDIT_WARNINGS -> text(player, "Warning minutes: 30,10,5,1", join(world(action.value()).warnings()), value ->
                    editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                            old, null, null, null, GuiConfigurationEditor.parseWarnings(value), null, null)));
            case CYCLE_SEED -> editWorld(player, action.value(), old -> {
                SeedPolicy[] values = SeedPolicy.values();
                SeedPolicy next = values[(old.regeneration().seedPolicy().ordinal() + 1) % values.length];
                RegenerationSettings regeneration = new RegenerationSettings(
                        next, next == SeedPolicy.FIXED ? Objects.requireNonNullElse(old.regeneration().fixedSeed(), 0L) : null,
                        old.regeneration().keepWorldConfig(), old.regeneration().keepGameRules(),
                        old.regeneration().keepWorldBorder());
                return GuiConfigurationEditor.copyWorld(old, null, null, null, null, regeneration, null);
            });
            case EDIT_FIXED_SEED -> longInput(player, "Fixed seed", world(action.value()).regeneration().fixedSeed(), value ->
                    editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(old, null, null, null, null,
                            new RegenerationSettings(SeedPolicy.FIXED, value, old.regeneration().keepWorldConfig(),
                                    old.regeneration().keepGameRules(), old.regeneration().keepWorldBorder()), null)));
            case TOGGLE_KEEP_CONFIG -> editRegeneration(player, action.value(), old -> new RegenerationSettings(
                    old.seedPolicy(), old.fixedSeed(), !old.keepWorldConfig(), old.keepGameRules(), old.keepWorldBorder()));
            case TOGGLE_KEEP_RULES -> editRegeneration(player, action.value(), old -> new RegenerationSettings(
                    old.seedPolicy(), old.fixedSeed(), old.keepWorldConfig(), !old.keepGameRules(), old.keepWorldBorder()));
            case TOGGLE_KEEP_BORDER -> editRegeneration(player, action.value(), old -> new RegenerationSettings(
                    old.seedPolicy(), old.fixedSeed(), old.keepWorldConfig(), old.keepGameRules(), !old.keepWorldBorder()));
            case TOGGLE_EVACUATION -> editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(
                    old, null, null, null, null, null,
                    new EvacuationSettings(!old.evacuation().enabled(), old.evacuation().destination())));
            case EDIT_EVACUATION -> text(player, "Evacuation Multiverse world",
                    printable(world(action.value()).evacuation().destination()), value -> {
                        requireRegistered(value, "Evacuation destination");
                        if (value.equalsIgnoreCase(world(action.value()).multiverseWorld())) {
                            throw new IllegalArgumentException("A world cannot evacuate into itself.");
                        }
                        editWorld(player, action.value(), old -> GuiConfigurationEditor.copyWorld(old, null, null,
                                null, null, null, new EvacuationSettings(old.evacuation().enabled(), value)));
                    });
            case CONFIRM_RESET -> openConfirmation(player, ConfirmKind.RESET, action.value());
            case CONFIRM_REMOVE -> openConfirmation(player, ConfirmKind.REMOVE_CONFIG, action.value());
            case EXECUTE_RESET -> executeReset(player, action.value());
            case EXECUTE_REMOVE -> result(player, editor.removeWorld(action.value()), () -> openDashboard(player, 0));
            case EDIT_TIMEZONE -> text(player, "IANA timezone", configs.current().timezone().getId(), value -> {
                try {
                    result(player, editor.setTimezone(ZoneId.of(value)), () -> openGlobal(player));
                } catch (DateTimeException exception) {
                    throw new IllegalArgumentException("Unknown IANA timezone.");
                }
            });
            case EDIT_HUB -> text(player, "Default hub MV world", configs.current().defaultHubWorld(), value -> {
                requireRegistered(value, "Default hub");
                result(player, editor.setHub(value), () -> openGlobal(player));
            });
            case EDIT_RETRIES -> integer(player, "Safe retry count", configs.current().resetPolicy().maxSafeRetries(),
                    0, 100, value -> result(player, editor.updateResetPolicy(old -> new ResetPolicySettings(
                            value, old.retryDelaySeconds(), old.broadcastCompletion())), () -> openGlobal(player)));
            case EDIT_RETRY_DELAY -> integer(player, "Retry delay seconds", configs.current().resetPolicy().retryDelaySeconds(),
                    0, 86400, value -> result(player, editor.updateResetPolicy(old -> new ResetPolicySettings(
                            old.maxSafeRetries(), value, old.broadcastCompletion())), () -> openGlobal(player)));
            case TOGGLE_BROADCAST -> result(player, editor.updateResetPolicy(old -> new ResetPolicySettings(
                    old.maxSafeRetries(), old.retryDelaySeconds(), !old.broadcastCompletion())), () -> openGlobal(player));
            case TOGGLE_DISCOVERY -> updateTeleport(player, old -> new TeleportSettings(
                    !old.autoDiscover(), old.defaultEnabled(), old.showLocked(), old.worlds()));
            case TOGGLE_DEFAULT_TELEPORT -> updateTeleport(player, old -> new TeleportSettings(
                    old.autoDiscover(), !old.defaultEnabled(), old.showLocked(), old.worlds()));
            case TOGGLE_SHOW_LOCKED -> updateTeleport(player, old -> new TeleportSettings(
                    old.autoDiscover(), old.defaultEnabled(), !old.showLocked(), old.worlds()));
            case TOGGLE_TELEPORT_WORLD -> updateTeleportWorld(player, action.value(), old -> new TeleportDestinationSettings(
                    !old.enabled(), old.permission()));
            case EDIT_PERMISSION -> text(player, "Permission (none to clear)",
                    printable(teleportWorld(action.value()).permission()), value -> updateTeleportWorld(
                            player, action.value(), old -> new TeleportDestinationSettings(
                                    old.enabled(), nullable(value))));
            case REMOVE_TELEPORT_OVERRIDE -> result(player, editor.removeTeleportOverride(action.value()),
                    () -> openTeleportWorld(player, action.value()));
            case CLOSE -> player.closeInventory();
        }
    }

    private void openDashboard(Player player, int requestedPage) {
        PluginSettings settings = configs.current();
        List<ManagedWorldSettings> worlds = List.copyOf(settings.worlds().values());
        int page = boundedPage(requestedPage, worlds.size());
        DashboardHolder holder = new DashboardHolder(page);
        Inventory inventory = inventory(holder, 54, Component.text("RWR 5 Admin - Dashboard"));
        holder.add(4, item(Material.MAP, "&6Managed worlds",
                "Page " + (page + 1) + '/' + pageCount(worlds.size()),
                worlds.size() + " configured world(s)", "Entries fill left to right"), null);
        List<ManagedWorldSettings> visible = page(worlds, page);
        for (int index = 0; index < visible.size(); index++) {
            ManagedWorldSettings world = visible.get(index);
            holder.add(CONTENT_SLOTS[index], item(
                    world.canReset() ? Material.GRASS_BLOCK : Material.BARRIER,
                    color(world.canReset()) + world.displayName(),
                    "ID: " + world.id(), "Multiverse: " + world.multiverseWorld(), "State: " + world.state(),
                    "Next: " + schedules.nextRun(world.id()).map(Object::toString).orElse("not scheduled"),
                    "Click to configure"), new GuiAction(ActionType.WORLD, world.id(), 0));
        }
        holder.add(45, item(Material.EMERALD, "&aAdd managed world", "Adds RWR configuration only."),
                new GuiAction(ActionType.ADD_WORLD, "", 0));
        holder.add(46, item(Material.COMPARATOR, "&eGlobal settings"), action(ActionType.GLOBAL));
        holder.add(47, item(Material.ENDER_PEARL, "&bTeleport administration"), action(ActionType.TELEPORTS));
        holder.add(48, item(Material.BOOK, "&fReset history"), action(ActionType.HISTORY));
        holder.add(49, item(Material.CLOCK, "&eReload configuration", "Atomically reloads and reschedules."),
                action(ActionType.RELOAD));
        navigation(holder, 52, 53, page, worlds.size(), ActionType.DASHBOARD);
        holder.add(50, item(Material.BARRIER, "&cClose"), action(ActionType.CLOSE));
        player.openInventory(inventory);
    }

    private void openAddWorld(Player player, int requestedPage) {
        PluginSettings settings = configs.current();
        Set<String> configured = new LinkedHashSet<>();
        settings.worlds().values().forEach(world -> configured.add(world.multiverseWorld().toLowerCase(Locale.ROOT)));
        List<WorldSnapshot> worlds = List.copyOf(gateway.registeredWorlds());
        int page = boundedPage(requestedPage, worlds.size());
        AddWorldHolder holder = new AddWorldHolder(page);
        Inventory inventory = inventory(holder, 54, Component.text("RWR 5 - Add Multiverse World"));
        List<WorldSnapshot> visible = page(worlds, page);
        for (int index = 0; index < visible.size(); index++) {
            WorldSnapshot world = visible.get(index);
            boolean protectedWorld = world.name().equalsIgnoreCase(settings.defaultHubWorld())
                    || world.name().equalsIgnoreCase(gateway.defaultWorldName());
            boolean exists = configured.contains(world.name().toLowerCase(Locale.ROOT));
            boolean addable = !protectedWorld && !exists;
            List<String> lore = List.of(
                    "Status: " + (world.loaded() ? "loaded" : "unloaded"),
                    "RWR: " + (exists ? "already configured" : protectedWorld ? "protected" : "available"),
                    addable ? "Click to add configuration only" : "Cannot be selected");
            int slot = CONTENT_SLOTS[index];
            holder.add(slot, item(addable ? Material.GRASS_BLOCK : Material.BARRIER,
                    (addable ? "&a" : "&c") + world.name(), lore.toArray(String[]::new)),
                    addable ? new GuiAction(ActionType.ADD_SELECTED, world.name(), 0) : null);
        }
        holder.add(49, item(Material.ARROW, "&eBack"), action(ActionType.DASHBOARD));
        navigation(holder, 52, 53, page, worlds.size(), ActionType.ADD_WORLD);
        player.openInventory(inventory);
    }

    private void openWorld(Player player, String id) {
        ManagedWorldSettings world = configs.current().world(id).orElse(null);
        if (world == null) {
            message(player, false, "That RWR configuration no longer exists.");
            openDashboard(player, 0);
            return;
        }
        WorldHolder holder = new WorldHolder(id);
        Inventory inv = inventory(holder, 54, Component.text("RWR 5 - " + truncate(world.displayName(), 22)));
        holder.add(0, item(Material.NAME_TAG, "&eDisplay name", world.displayName()), value(ActionType.EDIT_DISPLAY, id));
        holder.add(1, toggle("Scheduling enabled", world.enabled(), "State: " + world.state()), value(ActionType.TOGGLE_ENABLED, id));
        holder.add(2, item(Material.REPEATER, "&eSchedule type", world.schedule().type().name()), value(ActionType.CYCLE_SCHEDULE, id));
        holder.add(3, item(Material.CLOCK, "&eTime", printable(world.schedule().time()), "Used by daily/weekly/monthly"), value(ActionType.EDIT_TIME, id));
        if (world.schedule().type() == ScheduleType.WEEKLY) {
            holder.add(4, item(Material.PAPER, "&eDay of week", printable(world.schedule().dayOfWeek())), value(ActionType.CYCLE_WEEKDAY, id));
        } else if (world.schedule().type() == ScheduleType.MONTHLY) {
            holder.add(4, item(Material.PAPER, "&eDay of month", Integer.toString(world.schedule().dayOfMonth())), value(ActionType.EDIT_MONTHDAY, id));
        } else if (world.schedule().type() == ScheduleType.INTERVAL) {
            holder.add(4, item(Material.PAPER, "&eInterval minutes", Integer.toString(world.schedule().intervalMinutes())), value(ActionType.EDIT_INTERVAL, id));
        }
        holder.add(5, item(Material.BELL, "&eWarning minutes", join(world.warnings()),
                "Whole minutes before reset", "Example: 30,10,5,1", "Type 'none' to disable"),
                value(ActionType.EDIT_WARNINGS, id));
        holder.add(9, item(Material.WHEAT_SEEDS, "&eSeed policy", world.regeneration().seedPolicy().name()), value(ActionType.CYCLE_SEED, id));
        if (world.regeneration().seedPolicy() == SeedPolicy.FIXED) {
            holder.add(10, item(Material.PAPER, "&eFixed seed", printable(world.regeneration().fixedSeed())), value(ActionType.EDIT_FIXED_SEED, id));
        }
        holder.add(11, toggle("Keep Multiverse config", world.regeneration().keepWorldConfig()), value(ActionType.TOGGLE_KEEP_CONFIG, id));
        holder.add(12, toggle("Keep gamerules", world.regeneration().keepGameRules()), value(ActionType.TOGGLE_KEEP_RULES, id));
        holder.add(13, toggle("Keep world border", world.regeneration().keepWorldBorder()), value(ActionType.TOGGLE_KEEP_BORDER, id));
        holder.add(18, toggle("Evacuate players", world.evacuation().enabled()), value(ActionType.TOGGLE_EVACUATION, id));
        holder.add(19, item(Material.COMPASS, "&eEvacuation destination", printable(world.evacuation().destination())), value(ActionType.EDIT_EVACUATION, id));
        ResetPolicySettings policy = configs.current().resetPolicy();
        holder.add(20, item(Material.SHIELD, "&eFailure policy (global)",
                "Safe retries: " + policy.maxSafeRetries(), "Retry delay: " + policy.retryDelaySeconds() + "s",
                "Click to edit global policy"), action(ActionType.GLOBAL));
        holder.add(45, item(world.canReset() ? Material.TNT : Material.BARRIER, "&cReset now",
                world.canReset() ? "Requires confirmation" : "Blocked: " + world.state()),
                world.canReset() ? value(ActionType.CONFIRM_RESET, id) : null);
        holder.add(46, item(Material.LAVA_BUCKET, "&cRemove RWR configuration",
                "Does NOT delete the Multiverse world", "Requires confirmation"), value(ActionType.CONFIRM_REMOVE, id));
        holder.add(49, item(Material.ARROW, "&eBack"), action(ActionType.DASHBOARD));
        player.openInventory(inv);
    }

    private void openGlobal(Player player) {
        PluginSettings settings = configs.current();
        GlobalHolder holder = new GlobalHolder();
        Inventory inv = inventory(holder, 27, Component.text("RWR 5 - Global Settings"));
        holder.add(0, item(Material.CLOCK, "&eTimezone", settings.timezone().getId(), "Use an IANA ZoneId"), action(ActionType.EDIT_TIMEZONE));
        holder.add(1, item(Material.RECOVERY_COMPASS, "&eDefault protected hub", settings.defaultHubWorld(),
                "Cannot be an RWR-managed world"), action(ActionType.EDIT_HUB));
        holder.add(3, item(Material.SHIELD, "&eMaximum safe retries", Integer.toString(settings.resetPolicy().maxSafeRetries())), action(ActionType.EDIT_RETRIES));
        holder.add(4, item(Material.CLOCK, "&eRetry delay seconds", Integer.toString(settings.resetPolicy().retryDelaySeconds())), action(ActionType.EDIT_RETRY_DELAY));
        holder.add(5, toggle("Broadcast completion", settings.resetPolicy().broadcastCompletion()), action(ActionType.TOGGLE_BROADCAST));
        holder.add(9, toggle("Teleport auto-discovery", settings.teleport().autoDiscover()), action(ActionType.TOGGLE_DISCOVERY));
        holder.add(10, toggle("Discovered worlds enabled", settings.teleport().defaultEnabled()), action(ActionType.TOGGLE_DEFAULT_TELEPORT));
        holder.add(11, toggle("Show locked destinations", settings.teleport().showLocked()), action(ActionType.TOGGLE_SHOW_LOCKED));
        holder.add(22, item(Material.ARROW, "&eBack"), action(ActionType.DASHBOARD));
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
        Inventory inv = inventory(holder, 54, Component.text("RWR 5 - Teleport Admin"));
        List<String> visible = page(worlds, page);
        for (int index = 0; index < visible.size(); index++) {
            String name = visible.get(index);
            TeleportDestinationSettings destination = settings.worlds().getOrDefault(name,
                    new TeleportDestinationSettings(settings.defaultEnabled(), null));
            boolean loaded = gateway.world(name).map(WorldSnapshot::loaded).orElse(false);
            holder.add(CONTENT_SLOTS[index], item(destination.enabled() ? Material.ENDER_PEARL : Material.ENDER_EYE,
                    (destination.enabled() ? "&a" : "&c") + displayName(name),
                    "World: " + name, "Status: " + (loaded ? "loaded" : "unloaded/orphaned"),
                    "Permission: " + printable(destination.permission()),
                    settings.worlds().containsKey(name) ? "Explicit override" : "Discovery defaults"),
                    value(ActionType.TELEPORT_WORLD, name));
        }
        holder.add(45, toggle("Auto-discover", settings.autoDiscover()), action(ActionType.TOGGLE_DISCOVERY));
        holder.add(46, toggle("Default enabled", settings.defaultEnabled()), action(ActionType.TOGGLE_DEFAULT_TELEPORT));
        holder.add(47, toggle("Show locked", settings.showLocked()), action(ActionType.TOGGLE_SHOW_LOCKED));
        holder.add(49, item(Material.ARROW, "&eBack"), action(ActionType.DASHBOARD));
        navigation(holder, 52, 53, page, worlds.size(), ActionType.TELEPORTS);
        player.openInventory(inv);
    }

    private void openTeleportWorld(Player player, String worldName) {
        TeleportDestinationSettings destination = teleportWorld(worldName);
        TeleportWorldHolder holder = new TeleportWorldHolder(worldName);
        Inventory inv = inventory(holder, 27, Component.text("RWR Teleport - " + truncate(worldName, 18)));
        holder.add(0, toggle("Destination enabled", destination.enabled()), value(ActionType.TOGGLE_TELEPORT_WORLD, worldName));
        holder.add(1, item(Material.NAME_TAG, "&eShared display name", displayName(worldName),
                "Edit this in the managed-world dashboard", "Multiverse name remains: " + worldName), null);
        holder.add(2, item(Material.TRIPWIRE_HOOK, "&ePermission", printable(destination.permission()), "'none' clears it"), value(ActionType.EDIT_PERMISSION, worldName));
        holder.add(9, item(Material.BUCKET, "&eUse discovery defaults", "Removes only this teleport override"), value(ActionType.REMOVE_TELEPORT_OVERRIDE, worldName));
        if (gateway.world(worldName).isEmpty()) {
            holder.add(4, item(Material.BARRIER, "&cOrphaned override",
                    "World is not registered in Multiverse", "You may edit or remove this RWR entry"), null);
        }
        holder.add(22, item(Material.ARROW, "&eBack"), action(ActionType.TELEPORTS));
        player.openInventory(inv);
    }

    private void openHistory(Player player) {
        List<ResetHistoryEntry> entries = resets.recentHistory(45);
        HistoryHolder holder = new HistoryHolder();
        Inventory inv = inventory(holder, 54, Component.text("RWR 5 - Reset History"));
        for (ResetHistoryEntry entry : entries) {
            int slot = inv.firstEmpty();
            if (slot < 0 || slot >= 45) {
                break;
            }
            holder.add(slot, item(entry.failure() == null ? Material.LIME_DYE : Material.RED_DYE,
                    (entry.failure() == null ? "&a" : "&c")
                            + WorldDisplayNames.resolveId(configs.current(), entry.worldId()),
                    "Completed: " + entry.completedAt(), "Result: " + entry.terminalPhase(),
                    "Failure: " + printable(entry.failure()), truncate(entry.message(), 48)), null);
        }
        holder.add(49, item(Material.ARROW, "&eBack"), action(ActionType.DASHBOARD));
        player.openInventory(inv);
    }

    private void openConfirmation(Player player, ConfirmKind kind, String id) {
        ManagedWorldSettings world = configs.current().world(id).orElse(null);
        if (world == null || (kind == ConfirmKind.RESET && !world.canReset())) {
            message(player, false, "That action is no longer safe or available.");
            openDashboard(player, 0);
            return;
        }
        ConfirmationHolder holder = new ConfirmationHolder(kind, id);
        Inventory inv = inventory(holder, 27, Component.text(
                kind == ConfirmKind.RESET ? "Confirm guarded reset" : "Confirm RWR removal"));
        String[] lore = kind == ConfirmKind.RESET
                ? new String[] {"Runs the guarded reset coordinator", "Protected worlds cannot reach this screen"}
                : new String[] {"Removes RWR configuration ONLY", "The Multiverse world remains registered", "RWR never calls world deletion here"};
        holder.add(11, item(Material.RED_CONCRETE, "&cCONFIRM: " + (kind == ConfirmKind.RESET ? "Reset now" : "Remove RWR config"), lore),
                value(kind == ConfirmKind.RESET ? ActionType.EXECUTE_RESET : ActionType.EXECUTE_REMOVE, id));
        holder.add(15, item(Material.LIME_CONCRETE, "&aCancel"), value(ActionType.WORLD, id));
        player.openInventory(inv);
    }

    private void executeReset(Player player, String id) {
        ManagedWorldSettings world = configs.current().world(id).orElse(null);
        if (world == null || !world.canReset()) {
            message(player, false, "Reset blocked: the persisted world is protected, disabled, or unavailable.");
            openDashboard(player, 0);
            return;
        }
        player.closeInventory();
        message(player, true, "Starting guarded reset for " + world.displayName() + "...");
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ResetOutcome outcome = schedules.resetNow(id);
            message(player, outcome.successful(), outcome.message());
        });
    }

    private void reload(Player player) {
        var result = configs.reload();
        if (result.accepted()) {
            message(player, true, "Configuration reloaded; all GUI values and schedules were refreshed.");
        } else {
            message(player, false, "Reload rejected; the previous valid configuration remains active.");
            result.issues().forEach(issue -> messages.send(player, "gui.issue", "issue", issue));
        }
        openDashboard(player, 0);
    }

    private void editWorld(Player player, String id, java.util.function.UnaryOperator<ManagedWorldSettings> edit) {
        result(player, editor.updateWorld(id, edit, "World configuration saved."), () -> openWorld(player, id));
    }

    private void editRegeneration(Player player, String id, java.util.function.UnaryOperator<RegenerationSettings> edit) {
        editWorld(player, id, old -> GuiConfigurationEditor.copyWorld(
                old, null, null, null, null, edit.apply(old.regeneration()), null));
    }

    private void updateTeleport(Player player, java.util.function.UnaryOperator<TeleportSettings> edit) {
        result(player, editor.updateTeleport(edit, "Teleport defaults saved."), () -> openTeleports(player, 0));
    }

    private void updateTeleportWorld(Player player, String name,
            java.util.function.UnaryOperator<TeleportDestinationSettings> edit) {
        result(player, editor.updateTeleportWorld(name, edit, "Teleport destination saved."),
                () -> openTeleportWorld(player, name));
    }

    private void text(Player player, String title, String initial, Consumer<String> accepted) {
        input.request(player, title, initial, value -> {
            try {
                accepted.accept(value);
            } catch (NumberFormatException exception) {
                message(player, false, "Enter a valid whole number.");
            } catch (DateTimeParseException exception) {
                message(player, false, "Enter a valid 24-hour time in HH:mm format.");
            } catch (IllegalArgumentException exception) {
                message(player, false, exception.getMessage());
            }
        });
    }

    private void integer(Player player, String title, int initial, int minimum, int maximum, Consumer<Integer> accepted) {
        text(player, title, Integer.toString(initial), value -> {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException("Value must be from " + minimum + " through " + maximum + '.');
            }
            accepted.accept(parsed);
        });
    }

    private void longInput(Player player, String title, Long initial, Consumer<Long> accepted) {
        text(player, title, printable(initial), value -> accepted.accept(Long.parseLong(value)));
    }

    private void result(Player player, GuiEditResult edit, Runnable reopen) {
        message(player, edit.accepted(), edit.message());
        edit.issues().forEach(issue -> messages.send(player, "gui.issue", "issue", issue));
        reopen.run();
    }

    private ManagedWorldSettings world(String id) {
        return configs.current().world(id).orElseThrow(() -> new IllegalArgumentException("World configuration changed; reopen the GUI."));
    }

    private TeleportDestinationSettings teleportWorld(String name) {
        TeleportSettings teleport = configs.current().teleport();
        return teleport.worlds().getOrDefault(name,
                new TeleportDestinationSettings(teleport.defaultEnabled(), null));
    }

    private String displayName(String multiverseWorld) {
        return WorldDisplayNames.resolve(configs.current(), multiverseWorld);
    }

    private void openWorldByName(Player player, String name) {
        configs.current().worlds().values().stream()
                .filter(world -> world.multiverseWorld().equalsIgnoreCase(name)).findFirst()
                .ifPresentOrElse(world -> openWorld(player, world.id()), () -> openAddWorld(player, 0));
    }

    private void requireRegistered(String name, String field) {
        if (gateway.world(name).isEmpty()) {
            throw new IllegalArgumentException(field + " must name a registered Multiverse world.");
        }
    }

    private static ScheduleSettings defaultsFor(ScheduleType type, ScheduleSettings old) {
        LocalTime time = Objects.requireNonNullElse(old.time(), LocalTime.of(3, 0));
        return switch (type) {
            case DAILY -> new ScheduleSettings(type, time, null, 0, 0);
            case WEEKLY -> new ScheduleSettings(type, time, DayOfWeek.MONDAY, 0, 0);
            case MONTHLY -> new ScheduleSettings(type, time, null, 1, 0);
            case INTERVAL -> new ScheduleSettings(type, null, null, 0, Math.max(1, old.intervalMinutes()));
        };
    }

    private static String required(String value, String field) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }
        return value;
    }

    private static String nullable(String value) {
        return value.isBlank() || value.equalsIgnoreCase("none") ? null : value;
    }

    private static Inventory inventory(AdminHolder holder, int size, Component title) {
        Inventory inventory = Bukkit.createInventory(
                holder, size, LegacyComponentSerializer.legacySection().serialize(title));
        holder.attach(inventory);
        return inventory;
    }

    @SuppressWarnings("deprecation")
    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(ChatColor.GRAY + line);
        }
        meta.setLore(colored);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack toggle(String name, boolean enabled, String... extra) {
        List<String> lore = new ArrayList<>();
        lore.add(enabled ? "Enabled" : "Disabled");
        lore.addAll(List.of(extra));
        return item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? "&a" : "&c") + name, lore.toArray(String[]::new));
    }

    private static void navigation(AdminHolder holder, int previousSlot, int nextSlot, int page,
            int itemCount, ActionType type) {
        if (page > 0) {
            holder.add(previousSlot, item(Material.ARROW, "&ePrevious page"), new GuiAction(type, "", page - 1));
        }
        if ((page + 1) * PAGE_SIZE < itemCount) {
            holder.add(nextSlot, item(Material.ARROW, "&eNext page"), new GuiAction(type, "", page + 1));
        }
    }

    private static <T> List<T> page(List<T> values, int page) {
        int start = page * PAGE_SIZE;
        return values.subList(Math.min(start, values.size()), Math.min(start + PAGE_SIZE, values.size()));
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

    private static String join(List<Integer> values) {
        return values.isEmpty() ? "none" : values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("none");
    }

    private static String printable(Object value) {
        return value == null ? "none" : value.toString();
    }

    private static String truncate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum - 1) + "…";
    }

    private static String color(boolean positive) {
        return positive ? "&a" : "&c";
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private void message(Player player, boolean success, String message) {
        messages.send(player, success ? "gui.success" : "gui.failure", "message", message);
    }

    private enum ConfirmKind { RESET, REMOVE_CONFIG }

    private enum ActionType {
        DASHBOARD, ADD_WORLD, WORLD, ADD_SELECTED, GLOBAL, TELEPORTS, TELEPORT_WORLD, HISTORY, RELOAD,
        EDIT_DISPLAY, TOGGLE_ENABLED, CYCLE_SCHEDULE, EDIT_TIME, CYCLE_WEEKDAY, EDIT_MONTHDAY,
        EDIT_INTERVAL, EDIT_WARNINGS, CYCLE_SEED, EDIT_FIXED_SEED, TOGGLE_KEEP_CONFIG, TOGGLE_KEEP_RULES,
        TOGGLE_KEEP_BORDER, TOGGLE_EVACUATION, EDIT_EVACUATION, CONFIRM_RESET, CONFIRM_REMOVE,
        EXECUTE_RESET, EXECUTE_REMOVE, EDIT_TIMEZONE, EDIT_HUB, EDIT_RETRIES, EDIT_RETRY_DELAY,
        TOGGLE_BROADCAST, TOGGLE_DISCOVERY, TOGGLE_DEFAULT_TELEPORT, TOGGLE_SHOW_LOCKED,
        TOGGLE_TELEPORT_WORLD, EDIT_PERMISSION, REMOVE_TELEPORT_OVERRIDE, CLOSE
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

    private static final class DashboardHolder extends AdminHolder { DashboardHolder(int page) {} }
    private static final class AddWorldHolder extends AdminHolder { AddWorldHolder(int page) {} }
    private static final class WorldHolder extends AdminHolder { WorldHolder(String id) {} }
    private static final class GlobalHolder extends AdminHolder {}
    private static final class TeleportHolder extends AdminHolder { TeleportHolder(int page) {} }
    private static final class TeleportWorldHolder extends AdminHolder { TeleportWorldHolder(String name) {} }
    private static final class HistoryHolder extends AdminHolder {}
    private static final class ConfirmationHolder extends AdminHolder { ConfirmationHolder(ConfirmKind kind, String id) {} }
}
