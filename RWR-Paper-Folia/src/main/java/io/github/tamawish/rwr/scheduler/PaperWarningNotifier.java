package io.github.tamawish.rwr.scheduler;

import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.message.MessageService;
import java.time.ZonedDateTime;
import org.bukkit.Server;

/** Broadcasts advance reset warnings with Paper Adventure components. */
public final class PaperWarningNotifier implements WarningNotifier {
  private final Server server;
  private final MessageService messages;

  public PaperWarningNotifier(Server server, MessageService messages) {
    this.server = server;
    this.messages = messages;
  }

  @Override
  public void warn(ManagedWorldSettings world, int minutesRemaining, ZonedDateTime resetAt) {
    messages.broadcast(
        server,
        "notification.warning",
        "world",
        world.displayName(),
        "remaining",
        formatDuration(minutesRemaining),
        "reset_at",
        resetAt);
  }

  private String formatDuration(int minutes) {
    if (minutes == 0) {
      return messages.plain("value.now");
    }
    if (minutes % 60 == 0) {
      return messages.plain("value.duration-hours", "count", minutes / 60);
    }
    return messages.plain("value.duration-minutes", "count", minutes);
  }
}
