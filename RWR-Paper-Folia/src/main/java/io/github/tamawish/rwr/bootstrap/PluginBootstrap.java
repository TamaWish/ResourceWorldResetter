package io.github.tamawish.rwr.bootstrap;

import io.github.tamawish.rwr.ResourceWorldResetterPlugin;
import io.github.tamawish.rwr.bukkitapi.BukkitResetEventPublisher;
import io.github.tamawish.rwr.bukkitapi.BukkitRwrApi;
import io.github.tamawish.rwr.bukkitapi.BukkitWarningEventPublisher;
import io.github.tamawish.rwr.bukkitapi.RwrApiRegistration;
import io.github.tamawish.rwr.command.RwrCommand;
import io.github.tamawish.rwr.config.ConfigIssue;
import io.github.tamawish.rwr.config.ConfigLoadStatus;
import io.github.tamawish.rwr.config.ConfigRepository;
import io.github.tamawish.rwr.config.ConfigService;
import io.github.tamawish.rwr.config.ListenerRegistration;
import io.github.tamawish.rwr.config.WorldDisplayNames;
import io.github.tamawish.rwr.gui.AdminGuiService;
import io.github.tamawish.rwr.gui.GuiInputService;
import io.github.tamawish.rwr.gui.PlayerTeleportGui;
import io.github.tamawish.rwr.history.ResetJournal;
import io.github.tamawish.rwr.message.MessageService;
import io.github.tamawish.rwr.reset.FoliaPlayerEvacuationService;
import io.github.tamawish.rwr.reset.ResetCoordinator;
import io.github.tamawish.rwr.scheduler.FoliaOneShotTaskScheduler;
import io.github.tamawish.rwr.scheduler.NextRunCalculator;
import io.github.tamawish.rwr.scheduler.PaperResetNotifier;
import io.github.tamawish.rwr.scheduler.PaperWarningNotifier;
import io.github.tamawish.rwr.scheduler.ScheduleManager;
import io.github.tamawish.rwr.teleport.TeleportService;
import io.github.tamawish.rwr.worlds.WorldsLifecycleListener;
import io.github.tamawish.rwr.worlds.WorldsWorldProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.HandlerList;

/**
 * Wires rwr-core {@link ResetCoordinator}, {@link ScheduleManager}, and {@link ResetJournal}
 * with Paper/Folia adapters (Worlds provider, async evacuation, Adventure messages).
 */
public final class PluginBootstrap {
    private static final int BSTATS_PLUGIN_ID = 33605;

    private final ResourceWorldResetterPlugin plugin;
    private WorldsLifecycleListener lifecycleListener;
    private ScheduleManager scheduleManager;
    private ListenerRegistration configListener;
    private AdminGuiService adminGui;
    private GuiInputService guiInput;
    private PlayerTeleportGui playerTeleportGui;
    private MessageService messages;
    private Metrics metrics;
    private RwrApiRegistration apiRegistration;
    private CompletableFuture<?> updateCheck;

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

        WorldsWorldProvider gateway;
        try {
            gateway = new WorldsWorldProvider(plugin);
        } catch (IllegalStateException exception) {
            plugin.getLogger().severe(exception.getMessage());
            return false;
        }

        Path configFile = plugin.getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configFile)) {
            plugin.saveResource("config.yml", false);
        }
        ConfigService configService = new ConfigService(new ConfigRepository(configFile, gateway));
        ConfigService.ReloadResult initialLoad = configService.reload();
        if (!initialLoad.accepted()) {
            String prefix = initialLoad.status() == ConfigLoadStatus.MIGRATION_REQUIRED
                    ? "RWR v4 / pre-v5 configuration detected; automatic migration is intentionally disabled. "
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
                new FoliaPlayerEvacuationService(plugin, gateway, gateway.keys()),
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
                new FoliaOneShotTaskScheduler(plugin),
                new BukkitWarningEventPublisher(
                        plugin.getServer(), new PaperWarningNotifier(plugin.getServer(), messages)),
                Clock.systemUTC(),
                new PaperResetNotifier(plugin, plugin.getServer(), messages));
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
        playerTeleportGui = new PlayerTeleportGui(plugin, teleportService, messages);
        plugin.getServer().getPluginManager().registerEvents(playerTeleportGui, plugin);

        RwrCommand executor =
                new RwrCommand(plugin, configService, coordinator, scheduleManager, adminGui, playerTeleportGui, messages);
        PluginCommand command = Objects.requireNonNull(plugin.getCommand("rwr"), "rwr command");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        lifecycleListener = new WorldsLifecycleListener(configService, gateway, plugin.getLogger());
        plugin.getServer().getPluginManager().registerEvents(lifecycleListener, plugin);

        metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

        plugin.getLogger().info("[RWR] [INFO] ResourceWorldResetter-Paper-Folia 5 scheduler and guarded reset coordinator "
                + "enabled with Worlds " + dependency.installedVersion() + ". Loaded "
                + configService.current().worlds().size()
                + " configured world(s); scheduled " + scheduleManager.scheduledWorldCount() + " managed world(s).");
        startUpdateCheck(configFile);
        apiRegistration = RwrApiRegistration.register(
                plugin.getServer(), plugin, new BukkitRwrApi(configService, coordinator));
        return true;
    }

    private void logDependencyFailure(String message) {
        plugin.getLogger().severe("[RWR] [ERROR] Startup blocked: " + message);
        plugin.getLogger().severe("[RWR] [ACTION] Install Worlds 4.4.0+ for Paper/Purpur/Folia: "
                + DependencyValidator.DOWNLOAD_URL);
        plugin.getLogger().severe("[RWR] [INFO] RWR-Paper-Folia is disabled safely.");
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
        updateCheck = new GithubReleaseChecker().check(installed).thenAccept(result -> {
            if (!plugin.isEnabled()) {
                return;
            }
            if (result.status() == GithubReleaseChecker.Status.FAILURE) {
                plugin.getLogger().warning("[RWR] [WARN] " + result.message());
            } else if (result.status() == GithubReleaseChecker.Status.UPDATE_AVAILABLE) {
                plugin.getLogger().info("[RWR] [INFO] Update available: " + result.installed() + " -> "
                        + result.latest() + ". Download: " + GithubReleaseChecker.MODRINTH_URL);
                plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> plugin.getServer()
                        .getOnlinePlayers().stream()
                        .filter(player -> player.hasPermission("rwr.admin"))
                        .forEach(player -> player.sendMessage(Component.text("[RWR] Update available: "
                                                + result.latest() + "  ", NamedTextColor.GOLD)
                                        .append(Component.text(GithubReleaseChecker.MODRINTH_URL, NamedTextColor.AQUA)
                                                .clickEvent(ClickEvent.openUrl(GithubReleaseChecker.MODRINTH_URL))))));
            }
        });
    }

    public void disable() {
        if (updateCheck != null) {
            updateCheck.cancel(true);
            updateCheck = null;
        }
        if (apiRegistration != null) {
            apiRegistration.close();
            apiRegistration = null;
        }
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
