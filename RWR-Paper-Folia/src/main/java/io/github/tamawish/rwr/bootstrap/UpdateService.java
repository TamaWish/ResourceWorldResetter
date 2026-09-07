package io.github.tamawish.rwr.bootstrap;

import io.github.tamawish.rwr.ResourceWorldResetterPlugin;
import io.github.tamawish.rwr.message.MessageService;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Owns the single cached update check and permission-gated operator notifications. */
public final class UpdateService implements Listener, AutoCloseable {
  private final ResourceWorldResetterPlugin plugin;
  private final MessageService messages;
  private final GithubReleaseChecker checker;
  private final Path configFile;
  private PluginVersion installed;
  private volatile GithubReleaseChecker.Result cached;
  private CompletableFuture<?> running;
  private boolean enabled;
  private boolean notifyOnJoin;
  private Duration timeout;

  /**
   * Creates the update service using the GitHub release endpoint.
   *
   * @param plugin owning plugin
   * @param messages localized message service
   * @param configFile plugin configuration file
   */
  public UpdateService(
      ResourceWorldResetterPlugin plugin, MessageService messages, Path configFile) {
    this(plugin, messages, configFile, new GithubReleaseChecker());
  }

  UpdateService(
      ResourceWorldResetterPlugin plugin,
      MessageService messages,
      Path configFile,
      GithubReleaseChecker checker) {
    this.plugin = plugin;
    this.messages = messages;
    this.configFile = configFile;
    this.checker = checker;
    reloadSettings();
    try {
      installed = PluginVersion.parse(plugin.getDescription().getVersion());
    } catch (RuntimeException error) {
      cached = GithubReleaseChecker.Result.failure(error.getMessage());
    }
  }

  /** Registers join notifications and starts the configured initial update check. */
  public void start() {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    if (enabled) {
      check(null, true);
    } else {
      plugin.getLogger().info(messages.plain("update.disabled"));
    }
  }

  /** Reloads update-check settings while retaining the last cached result. */
  public void reloadSettings() {
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile.toFile());
    enabled =
        config.getBoolean("updates.enabled", config.getBoolean("update-checker.enabled", true));
    notifyOnJoin = config.getBoolean("updates.notify-admins-on-join", true);
    int seconds = Math.max(1, Math.min(60, config.getInt("updates.request-timeout-seconds", 10)));
    timeout = Duration.ofSeconds(seconds);
  }

  /**
   * Reports the cached update state or starts a check for the requester.
   *
   * @param sender command sender receiving the result
   */
  public void report(CommandSender sender) {
    if (!enabled) {
      messages.send(sender, "update.disabled");
      return;
    }
    GithubReleaseChecker.Result result = cached;
    if (result == null) {
      messages.send(sender, "update.checking");
      check(sender, false);
    } else {
      sendResult(sender, result);
    }
  }

  private synchronized void check(CommandSender requester, boolean startup) {
    if (running != null && !running.isDone()) {
      return;
    }
    if (installed == null) {
      if (requester != null) {
        messages.send(requester, "update.failed");
      }
      return;
    }
    running =
        checker
            .check(installed, timeout)
            .thenAccept(
                result -> {
                  cached = result;
                  if (!plugin.isEnabled()) {
                    return;
                  }
                  if (requester instanceof Player player) {
                    player.getScheduler().run(plugin, ignored -> sendResult(player, result), null);
                  } else {
                    plugin
                        .getServer()
                        .getGlobalRegionScheduler()
                        .execute(
                            plugin,
                            () -> {
                              if (requester != null) {
                                sendResult(requester, result);
                              }
                              if (startup) {
                                announceStartup(result);
                              }
                            });
                  }
                });
  }

  private void announceStartup(GithubReleaseChecker.Result result) {
    if (result.status() == GithubReleaseChecker.Status.FAILURE) {
      plugin.getLogger().warning(messages.plain("log.update-failed", "reason", result.message()));
    } else if (result.status() == GithubReleaseChecker.Status.UPDATE_AVAILABLE) {
      plugin
          .getLogger()
          .info(
              messages.plain(
                  "log.update-available",
                  "installed",
                  result.installed(),
                  "latest",
                  result.latest(),
                  "url",
                  GithubReleaseChecker.RELEASES_URL));
      plugin.getServer().getOnlinePlayers().stream()
          .filter(p -> p.hasPermission("rwr.admin"))
          .forEach(p -> p.getScheduler().run(plugin, ignored -> sendAvailable(p, result), null));
    }
  }

  /**
   * Notifies authorized players when a cached update is available.
   *
   * @param event player join event
   */
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    GithubReleaseChecker.Result result = cached;
    if (enabled
        && notifyOnJoin
        && result != null
        && result.status() == GithubReleaseChecker.Status.UPDATE_AVAILABLE
        && event.getPlayer().hasPermission("rwr.admin")) {
      sendAvailable(event.getPlayer(), result);
    }
  }

  private void sendResult(CommandSender sender, GithubReleaseChecker.Result result) {
    switch (result.status()) {
      case UPDATE_AVAILABLE -> {
        if (sender instanceof Player player) {
          sendAvailable(player, result);
        } else {
          messages.send(
              sender,
              "update.available",
              "installed",
              result.installed(),
              "latest",
              result.latest(),
              "url",
              GithubReleaseChecker.RELEASES_URL);
        }
      }
      case UP_TO_DATE -> messages.send(sender, "update.current", "installed", result.installed());
      case FAILURE -> messages.send(sender, "update.failed");
      default -> throw new AssertionError("Unhandled update status: " + result.status());
    }
  }

  private void sendAvailable(Player player, GithubReleaseChecker.Result result) {
    Component component =
        messages
            .component(
                "update.available",
                "installed",
                result.installed(),
                "latest",
                result.latest(),
                "url",
                GithubReleaseChecker.RELEASES_URL)
            .clickEvent(ClickEvent.openUrl(GithubReleaseChecker.RELEASES_URL));
    player.sendMessage(component);
  }

  @Override
  public synchronized void close() {
    if (running != null) {
      running.cancel(true);
      running = null;
    }
    org.bukkit.event.HandlerList.unregisterAll(this);
  }
}
