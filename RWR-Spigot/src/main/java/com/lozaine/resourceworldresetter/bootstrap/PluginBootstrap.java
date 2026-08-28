package com.lozaine.resourceworldresetter.bootstrap;

import com.lozaine.resourceworldresetter.ResourceWorldResetterPlugin;
import com.lozaine.resourceworldresetter.command.RwrCommand;
import com.lozaine.resourceworldresetter.config.ConfigIssue;
import com.lozaine.resourceworldresetter.config.ConfigLoadStatus;
import com.lozaine.resourceworldresetter.config.ListenerRegistration;
import com.lozaine.resourceworldresetter.config.WorldDisplayNames;
import com.lozaine.resourceworldresetter.config.ConfigRepository;
import com.lozaine.resourceworldresetter.config.ConfigService;
import com.lozaine.resourceworldresetter.multiverse.MultiverseApiGateway;
import com.lozaine.resourceworldresetter.multiverse.MultiverseLifecycleListener;
import com.lozaine.resourceworldresetter.message.MessageService;
import com.lozaine.resourceworldresetter.history.ResetJournal;
import com.lozaine.resourceworldresetter.gui.AdminGuiService;
import com.lozaine.resourceworldresetter.gui.GuiInputService;
import com.lozaine.resourceworldresetter.gui.PlayerTeleportGui;
import com.lozaine.resourceworldresetter.reset.BukkitPlayerEvacuationService;
import com.lozaine.resourceworldresetter.reset.BukkitResetEventPublisher;
import com.lozaine.resourceworldresetter.reset.ResetCoordinator;
import com.lozaine.resourceworldresetter.scheduler.BukkitOneShotTaskScheduler;
import com.lozaine.resourceworldresetter.scheduler.BukkitResetNotifier;
import com.lozaine.resourceworldresetter.scheduler.BukkitWarningNotifier;
import com.lozaine.resourceworldresetter.scheduler.NextRunCalculator;
import com.lozaine.resourceworldresetter.scheduler.ScheduleManager;
import com.lozaine.resourceworldresetter.teleport.TeleportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.bstats.bukkit.Metrics;

public final class PluginBootstrap {
    private static final int BSTATS_PLUGIN_ID = 31502;
    private final ResourceWorldResetterPlugin plugin;
    private MultiverseLifecycleListener lifecycleListener;
    private ScheduleManager scheduleManager;
    private ListenerRegistration configListener;
    private AdminGuiService adminGui;
    private GuiInputService guiInput;
    private PlayerTeleportGui playerTeleportGui;
    private MessageService messages;
    private Metrics metrics;

    public PluginBootstrap(ResourceWorldResetterPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enable() {
        DependencyValidator.ValidationResult dependency =
                new DependencyValidator(plugin.getServer().getPluginManager()).validate();
        if (!dependency.compatible()) {
            logDependencyFailure(dependency.message());
            return false;
        }

        MultiverseApiGateway gateway = new MultiverseApiGateway(MultiverseCoreApi.get());
        Path configFile = plugin.getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configFile)) {
            plugin.saveResource("config.yml", false);
        }
        ConfigService configService = new ConfigService(new ConfigRepository(configFile, gateway));
        ConfigService.ReloadResult initialLoad = configService.reload();
        if (!initialLoad.accepted()) {
            String prefix = initialLoad.status() == ConfigLoadStatus.MIGRATION_REQUIRED
                    ? "RWR v4 configuration detected; automatic migration is intentionally disabled. "
                    : "RWR v5 configuration is invalid. ";
            plugin.getLogger().severe(prefix + "The plugin will remain disabled.");
            for (ConfigIssue issue : initialLoad.issues()) {
                plugin.getLogger().severe(" - " + issue);
            }
            return false;
        }

        ResetJournal journal;
        try {
            journal = new ResetJournal(plugin.getDataFolder().toPath(), 100, Clock.systemUTC());
        } catch (IOException exception) {
            plugin.getLogger().severe("Reset safety journal could not be opened; refusing to enable: "
                    + exception.getMessage());
            return false;
        }
        messages = new MessageService(plugin);
        ResetCoordinator coordinator = new ResetCoordinator(
                configService::current,
                gateway,
                new BukkitPlayerEvacuationService(plugin.getServer(), gateway),
                journal,
                Clock.systemUTC(),
                plugin.getLogger(),
                new BukkitResetEventPublisher(plugin.getServer()));
        try {
            int recovered = coordinator.recoverInterruptedOperations().size();
            if (recovered > 0) {
                plugin.getLogger().warning(recovered
                        + " interrupted reset(s) were recorded for administrator review; none were resumed.");
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Interrupted reset recovery could not be persisted; refusing to enable: "
                    + exception.getMessage());
            return false;
        }

        scheduleManager = new ScheduleManager(
                configService::current,
                coordinator,
                new NextRunCalculator(),
                new BukkitOneShotTaskScheduler(plugin),
                new BukkitWarningNotifier(plugin.getServer(), messages),
                Clock.systemUTC(),
                new BukkitResetNotifier(plugin.getServer(), messages));
        configListener = configService.addChangeListener(scheduleManager::replaceSchedules);
        scheduleManager.replaceSchedules(configService.current());

        guiInput = new GuiInputService(plugin, messages);
        adminGui = new AdminGuiService(
                plugin, configService, gateway, coordinator, scheduleManager, guiInput, messages);
        plugin.getServer().getPluginManager().registerEvents(guiInput, plugin);
        plugin.getServer().getPluginManager().registerEvents(adminGui, plugin);

        TeleportService teleportService = new TeleportService(
                () -> configService.current().teleport(),
                gateway,
                coordinator,
                name -> WorldDisplayNames.resolve(configService.current(), name));
        playerTeleportGui = new PlayerTeleportGui(teleportService, messages);
        plugin.getServer().getPluginManager().registerEvents(playerTeleportGui, plugin);

        RwrCommand executor =
                new RwrCommand(configService, coordinator, scheduleManager, adminGui, playerTeleportGui, messages);
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("rwr"), "rwr command");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        lifecycleListener = new MultiverseLifecycleListener(configService, gateway, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(lifecycleListener, plugin);

        metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

        plugin.getLogger().info("[RWR] [INFO] RWR 5 scheduler and guarded reset coordinator enabled with Multiverse-Core "
                + dependency.installedVersion() + ". Loaded " + configService.current().worlds().size()
                + " configured world(s); scheduled " + scheduleManager.scheduledWorldCount() + " managed world(s).");
        startUpdateCheck(configFile);
        return true;
    }

    private void logDependencyFailure(String message) {
        plugin.getLogger().severe("[RWR] [ERROR] Startup blocked: " + message);
        plugin.getLogger().severe("[RWR] [ACTION] Install Multiverse-Core 5.8.0 through 5.x: "
                + DependencyValidator.DOWNLOAD_URL);
        plugin.getLogger().severe("[RWR] [INFO] RWR-Spigot is disabled safely.");
    }

    private void startUpdateCheck(Path configFile) {
        if (!YamlConfiguration.loadConfiguration(configFile.toFile()).getBoolean("update-checker.enabled", true)) {
            plugin.getLogger().info("[RWR] [INFO] GitHub update check is disabled by configuration.");
            return;
        }
        PluginVersion installed;
        try {
            installed = PluginVersion.parse(plugin.getDescription().getVersion());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("[RWR] [WARN] GitHub update check skipped: " + exception.getMessage());
            return;
        }
        new GithubReleaseChecker().check(installed).thenAccept(result -> {
            if (result.status() == GithubReleaseChecker.Status.FAILURE) {
                plugin.getLogger().warning("[RWR] [WARN] " + result.message());
            } else if (result.status() == GithubReleaseChecker.Status.UPDATE_AVAILABLE) {
                plugin.getLogger().info("[RWR] [INFO] Update available: " + result.installed() + " -> "
                        + result.latest() + ". Download: " + GithubReleaseChecker.MODRINTH_URL);
                plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> player.hasPermission("rwr.admin"))
                        .forEach(player -> player.sendMessage(ChatColor.GOLD + "[RWR] Update available: "
                                + result.latest() + "  " + ChatColor.AQUA + GithubReleaseChecker.MODRINTH_URL)));
            }
        });
    }

    public void disable() {
        if (configListener != null) {
            configListener.unregister();
            configListener = null;
        }
        if (scheduleManager != null) {
            scheduleManager.close();
            scheduleManager = null;
        }
        if (lifecycleListener != null) {
            HandlerList.unregisterAll(lifecycleListener);
            lifecycleListener = null;
        }
        if (adminGui != null) {
            HandlerList.unregisterAll(adminGui);
            adminGui = null;
        }
        if (guiInput != null) {
            HandlerList.unregisterAll(guiInput);
            guiInput.close();
            guiInput = null;
        }
        if (playerTeleportGui != null) {
            HandlerList.unregisterAll(playerTeleportGui);
            playerTeleportGui = null;
        }
        if (messages != null) {
            messages.close();
            messages = null;
        }
        metrics = null;
    }
}
