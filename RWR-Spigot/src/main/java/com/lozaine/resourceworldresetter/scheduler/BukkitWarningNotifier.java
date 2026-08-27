package com.lozaine.resourceworldresetter.scheduler;

import com.lozaine.resourceworldresetter.config.ManagedWorldSettings;
import com.lozaine.resourceworldresetter.message.MessageService;
import java.time.ZonedDateTime;
import org.bukkit.Server;

public final class BukkitWarningNotifier implements WarningNotifier {
    private final Server server;
    private final MessageService messages;

    public BukkitWarningNotifier(Server server, MessageService messages) {
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

    private static String formatDuration(int minutes) {
        if (minutes == 0) {
            return "now";
        }
        if (minutes % 60 == 0) {
            return minutes / 60 + " hour(s)";
        }
        return minutes + " minute(s)";
    }
}
