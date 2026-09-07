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
import io.github.tamawish.rwr.multiverse.MultiverseApiGateway;
import io.github.tamawish.rwr.multiverse.MultiverseLifecycleListener;
import io.github.tamawish.rwr.reset.BukkitPlayerEvacuationService;
import io.github.tamawish.rwr.reset.ResetCoordinator;
import io.github.tamawish.rwr.scheduler.BukkitOneShotTaskScheduler;
import io.github.tamawish.rwr.scheduler.BukkitResetNotifier;
import io.github.tamawish.rwr.scheduler.BukkitWarningNotifier;
import io.github.tamawish.rwr.scheduler.NextRunCalculator;
import io.github.tamawish.rwr.scheduler.ScheduleManager;
import io.github.tamawish.rwr.teleport.TeleportService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.mvplugins.multiverse.core.MultiverseCoreApi;

/** Constructs and owns the Spigot services used by the plugin entry point. */
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
  private RwrApiRegistration apiRegistration;
  private UpdateService updates;

  public PluginBootstrap(ResourceWorldResetterPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Validates dependencies and constructs all Spigot services.
   *
   * @return whether startup completed successfully
   */
  public boolean enable() {
    DependencyValidator.ValidationResult dependency =
        new DependencyValidator(plugin.getServer().getPluginManager()).validate();
    if (!dependency.compatible()) {
      logDependencyFailure(dependency.message());
      return false;
    }

    MultiverseApiGateway gateway = new MultiverseApiGateway(MultiverseCoreApi.get());
    Path configFile = plugin.getDataFolder().toPath().resolve("config.yml");
    boolean configCreated = !Files.exists(configFile);
    if (configCreated) {
      plugin.saveResource("config.yml", false);
    }
    Path managedWorldsFile = plugin.getDataFolder().toPath().resolve("managed-worlds.yml");
    if (configCreated && !Files.exists(managedWorldsFile)) {
      plugin.saveResource("managed-worlds.yml", false);
    }
    ConfigRepository configRepository = new ConfigRepository(configFile, gateway);
    ConfigService configService = new ConfigService(configRepository);
    ConfigService.ReloadResult initialLoad = configService.reload();
    if (!initialLoad.accepted()) {
      String prefix =
          initialLoad.status() == ConfigLoadStatus.MIGRATION_REQUIRED
              ? "RWR v4 configuration detected; automatic migration is intentionally disabled. "
              : "RWR v5 configuration is invalid. ";
      plugin.getLogger().severe(prefix + "The plugin will remain disabled.");
      for (ConfigIssue issue : initialLoad.issues()) {
        plugin.getLogger().severe(" - " + issue);
      }
      return false;
    }
    try {
      configRepository.initializeManagedWorldsFile();
    } catch (IOException | io.github.tamawish.rwr.config.ConfigValidationException exception) {
      plugin
          .getLogger()
          .severe("managed-worlds.yml could not be initialized: " + exception.getMessage());
      return false;
    }

    ResetJournal journal;
    try {
      journal = new ResetJournal(plugin.getDataFolder().toPath(), 100, Clock.systemUTC());
    } catch (IOException exception) {
      plugin
          .getLogger()
          .severe(
              "Reset safety journal could not be opened; refusing to enable: "
                  + exception.getMessage());
      return false;
    }
    messages = new MessageService(plugin);
    ResetCoordinator coordinator =
        new ResetCoordinator(
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
        plugin
            .getLogger()
            .warning(
                recovered
                    + " interrupted reset(s) were recorded for administrator review; "
                    + "none were resumed.");
      }
    } catch (IOException exception) {
      plugin
          .getLogger()
          .severe(
              "Interrupted reset recovery could not be persisted; refusing to enable: "
                  + exception.getMessage());
      return false;
    }

    scheduleManager =
        new ScheduleManager(
            configService::current,
            coordinator,
            new NextRunCalculator(),
            new BukkitOneShotTaskScheduler(plugin),
            new BukkitWarningEventPublisher(
                plugin.getServer(), new BukkitWarningNotifier(plugin.getServer(), messages)),
            Clock.systemUTC(),
            new BukkitResetNotifier(plugin.getServer(), messages));
    scheduleManager.restoreSafetyHolds(coordinator.recentHistory(Integer.MAX_VALUE));
    configListener = configService.addChangeListener(scheduleManager::replaceSchedules);
    scheduleManager.replaceSchedules(configService.current());

    guiInput = new GuiInputService(plugin, messages);
    adminGui =
        new AdminGuiService(
            plugin, configService, gateway, coordinator, scheduleManager, guiInput, messages);
    plugin.getServer().getPluginManager().registerEvents(guiInput, plugin);
    plugin.getServer().getPluginManager().registerEvents(adminGui, plugin);

    TeleportService teleportService =
        new TeleportService(
            () -> configService.current().teleport(),
            gateway,
            coordinator,
            name -> WorldDisplayNames.resolve(configService.current(), name));
    playerTeleportGui = new PlayerTeleportGui(teleportService, messages);
    plugin.getServer().getPluginManager().registerEvents(playerTeleportGui, plugin);

    updates = new UpdateService(plugin, messages, configFile);
    RwrCommand executor =
        new RwrCommand(
            configService,
            coordinator,
            scheduleManager,
            adminGui,
            playerTeleportGui,
            messages,
            updates);
    PluginCommand command = Objects.requireNonNull(plugin.getCommand("rwr"), "rwr command");
    command.setExecutor(executor);
    command.setTabCompleter(executor);

    lifecycleListener = new MultiverseLifecycleListener(configService, gateway, plugin.getLogger());
    plugin.getServer().getPluginManager().registerEvents(lifecycleListener, plugin);

    metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

    plugin
        .getLogger()
        .info(
            "[RWR] [INFO] RWR 5 scheduler and guarded reset coordinator enabled with "
                + "Multiverse-Core "
                + dependency.installedVersion()
                + ". Loaded "
                + configService.current().worlds().size()
                + " configured world(s); scheduled "
                + scheduleManager.scheduledWorldCount()
                + " managed world(s).");
    updates.start();
    apiRegistration =
        RwrApiRegistration.register(
            plugin.getServer(), plugin, new BukkitRwrApi(configService, coordinator));
    return true;
  }

  private void logDependencyFailure(String message) {
    plugin.getLogger().severe("[RWR] [ERROR] Startup blocked: " + message);
    plugin
        .getLogger()
        .severe(
            "[RWR] [ACTION] Install Multiverse-Core 5.8.0 through 5.x: "
                + DependencyValidator.DOWNLOAD_URL);
    plugin.getLogger().severe("[RWR] [INFO] RWR-Spigot is disabled safely.");
  }

  /** Stops registered services and releases platform resources. */
  public void disable() {
    if (updates != null) {
      updates.close();
      updates = null;
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
