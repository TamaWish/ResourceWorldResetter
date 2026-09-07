package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.message.MessageService;
import io.github.tamawish.rwr.reset.ResetOutcome;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

/** Broadcasts terminal reset outcomes with Paper Adventure components. */
public final class PaperResetNotifier implements ResetNotifier {
  private final Server server;
  private final MessageService messages;
  private final Plugin plugin;

  /**
   * Creates a terminal notifier.
   *
   * @param plugin owning plugin
   * @param server Paper server receiving broadcasts
   * @param messages localized message service
   */
  public PaperResetNotifier(Plugin plugin, Server server, MessageService messages) {
    this.plugin = plugin;
    this.server = server;
    this.messages = messages;
  }

  @Override
  public void terminal(
      ManagedWorldSettings world, ResetOutcome outcome, boolean broadcastCompletion) {
    if (outcome.successful()) {
      if (broadcastCompletion) {
        messages.broadcast(server, "notification.reset-complete", "world", world.displayName());
      }
      return;
    }
    server.getOnlinePlayers().stream()
        .filter(player -> player.hasPermission("rwr.admin"))
        .forEach(
            player ->
                player
                    .getScheduler()
                    .run(
                        plugin,
                        ignored ->
                            messages.send(
                                player,
                                "notification.reset-failed",
                                "world",
                                world.displayName(),
                                "failure",
                                messages.plain(
                                    "value.failure."
                                        + outcome
                                            .failure()
                                            .name()
                                            .toLowerCase(java.util.Locale.ROOT))),
                        null));
  }
}
