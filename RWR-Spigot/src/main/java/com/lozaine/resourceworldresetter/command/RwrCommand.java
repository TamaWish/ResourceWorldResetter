package com.lozaine.resourceworldresetter.command;

import com.lozaine.resourceworldresetter.config.ConfigIssue;
import com.lozaine.resourceworldresetter.config.ConfigService;
import com.lozaine.resourceworldresetter.config.WorldDisplayNames;
import com.lozaine.resourceworldresetter.gui.AdminGuiService;
import com.lozaine.resourceworldresetter.gui.PlayerTeleportGui;
import com.lozaine.resourceworldresetter.message.MessageService;
import com.lozaine.resourceworldresetter.reset.ResetCoordinator;
import com.lozaine.resourceworldresetter.reset.ResetOutcome;
import com.lozaine.resourceworldresetter.reset.ResetStatus;
import com.lozaine.resourceworldresetter.scheduler.ScheduleManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RwrCommand implements CommandExecutor, TabCompleter {
    private static final DateTimeFormatter CHAT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private static final List<CommandHelp> COMMANDS = List.of(
            new CommandHelp("help", "/rwr help", "Show this help", "rwr.status", false),
            new CommandHelp("status", "/rwr status [id]", "Show reset status", "rwr.status", false),
            new CommandHelp("gui", "/rwr gui", "Open the administration GUI", "rwr.admin", true),
            new CommandHelp("tp", "/rwr tp", "Open the teleport GUI", "rwr.tp", true),
            new CommandHelp("reset", "/rwr reset <id>", "Request a guarded reset", "rwr.reset", false),
            new CommandHelp("history", "/rwr history [count]", "Show reset history", "rwr.history", false),
            new CommandHelp("reload", "/rwr reload", "Reload configuration", "rwr.reload", false));

    private final ConfigService configs;
    private final ResetCoordinator resets;
    private final ScheduleManager schedules;
    private final AdminGuiService adminGui;
    private final PlayerTeleportGui teleportGui;
    private final MessageService messages;

    public RwrCommand(
            ConfigService configs,
            ResetCoordinator resets,
            ScheduleManager schedules,
            AdminGuiService adminGui,
            PlayerTeleportGui teleportGui,
            MessageService messages) {
        this.configs = configs;
        this.resets = resets;
        this.schedules = schedules;
        this.adminGui = adminGui;
        this.teleportGui = teleportGui;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "gui" -> playerGui(sender, args, "rwr.admin", "admin GUI", adminGui::open);
            case "tp" -> playerGui(sender, args, "rwr.tp", "teleport GUI", teleportGui::open);
            case "reload" -> {
                if (exactAndPermitted(sender, args, 1, "rwr.reload")) {
                    reload(sender);
                }
            }
            case "reset" -> {
                if (exactAndPermitted(sender, args, 2, "rwr.reset")) {
                    reset(sender, args[1]);
                }
            }
            case "status" -> {
                if ((args.length == 1 || args.length == 2) && permitted(sender, "rwr.status")) {
                    status(sender, args.length == 2 ? args[1] : null);
                } else if (args.length > 2) {
                    usage(sender, "status");
                }
            }
            case "history" -> {
                if ((args.length == 1 || args.length == 2) && permitted(sender, "rwr.history")) {
                    history(sender, args.length == 2 ? args[1] : "10");
                } else if (args.length > 2) {
                    usage(sender, "history");
                }
            }
            default -> messages.send(sender, "command.unknown");
        }
        return true;
    }

    private void help(CommandSender sender) {
        messages.send(sender, "command.help-header");
        COMMANDS.stream().filter(entry -> visibleTo(sender, entry)).forEach(entry -> messages.send(
                sender, "command.help-entry", "usage", entry.usage(), "description", entry.description()));
    }

    private void playerGui(
            CommandSender sender,
            String[] args,
            String permission,
            String feature,
            java.util.function.Consumer<Player> opener) {
        if (!exactAndPermitted(sender, args, 1, permission)) {
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.player-only", "feature", feature);
            return;
        }
        opener.accept(player);
    }

    private void reload(CommandSender sender) {
        ConfigService.ReloadResult result = configs.reload();
        if (result.accepted()) {
            messages.send(sender, "command.reload-success", "count", configs.current().worlds().size());
            return;
        }
        messages.send(
                sender,
                "command.reload-failed",
                "retention",
                result.retainedPrevious() ? "the previous valid configuration remains active." : "no configuration is active.");
        for (ConfigIssue issue : result.issues()) {
            messages.send(sender, "command.issue", "issue", issue);
        }
    }

    private void reset(CommandSender sender, String worldId) {
        messages.send(sender, "command.reset-start", "world", displayName(worldId), "world_id", worldId);
        ResetOutcome outcome = schedules.resetNow(worldId);
        if (outcome.successful()) {
            messages.send(
                    sender,
                    "command.reset-success",
                    "operation",
                    outcome.operationId(),
                    "message",
                    outcome.message());
        } else {
            messages.send(
                    sender,
                    "command.reset-failed",
                    "failure",
                    outcome.failure(),
                    "safety",
                    outcome.safety(),
                    "operation",
                    outcome.operationId(),
                    "message",
                    outcome.message());
        }
    }

    private void status(CommandSender sender, String worldId) {
        if (worldId != null) {
            sendStatus(sender, resets.status(worldId));
        } else if (configs.current().worlds().isEmpty()) {
            messages.send(sender, "command.no-worlds");
        } else {
            configs.current().worlds().keySet().forEach(id -> sendStatus(sender, resets.status(id)));
        }
    }

    private void history(CommandSender sender, String value) {
        int count;
        try {
            count = Math.max(1, Math.min(50, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            messages.send(sender, "command.history-count");
            return;
        }
        var entries = resets.recentHistory(count);
        if (entries.isEmpty()) {
            messages.send(sender, "command.history-empty");
            return;
        }
        messages.send(sender, "command.history-header", "count", entries.size());
        entries.forEach(entry -> {
            messages.send(sender, "command.history-entry", "world", displayName(entry.worldId()),
                    "world_id", entry.worldId(), "phase", entry.terminalPhase());
            messages.send(sender, "command.history-time", "completed", formatTimestamp(entry.completedAt()),
                    "operation", shortOperation(entry.operationId()));
            messages.send(sender, "command.history-detail",
                    "result", entry.failure() == null ? "Successful" : entry.failure() + " / " + entry.safety(),
                    "message", entry.message());
        });
    }

    private void sendStatus(CommandSender sender, ResetStatus status) {
        messages.send(sender, "command.status-header", "world", displayName(status.worldId()),
                "world_id", status.worldId());
        messages.send(sender, "command.status-state", "phase", status.phase(), "world", status.multiverseWorld());
        messages.send(sender, "command.status-schedule", "next", schedules.nextRun(status.worldId())
                .map(next -> CHAT_TIME.format(next.withZoneSameInstant(configs.current().timezone())))
                .orElse("Not scheduled"));
        messages.send(sender, "command.status-operation", "operation", shortOperation(status.operationId()),
                "message", status.message());
    }

    private String displayName(String worldId) {
        return WorldDisplayNames.resolveId(configs.current(), worldId);
    }

    private String formatTimestamp(String timestamp) {
        try {
            ZoneId zone = configs.current().timezone();
            return CHAT_TIME.format(Instant.parse(timestamp).atZone(zone));
        } catch (RuntimeException exception) {
            return timestamp;
        }
    }

    private static String shortOperation(String operationId) {
        if (operationId == null || operationId.equalsIgnoreCase("none") || operationId.length() <= 8) {
            return operationId == null ? "none" : operationId;
        }
        return operationId.substring(0, 8);
    }

    private boolean exactAndPermitted(CommandSender sender, String[] args, int count, String permission) {
        if (args.length != count) {
            usage(sender, args[0]);
            return false;
        }
        return permitted(sender, permission);
    }

    private boolean permitted(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "command.no-permission", "permission", permission);
        return false;
    }

    private void usage(CommandSender sender, String name) {
        COMMANDS.stream().filter(entry -> entry.name().equalsIgnoreCase(name)).findFirst().ifPresent(entry ->
                messages.send(sender, "command.help-entry", "usage", entry.usage(), "description", entry.description()));
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            return matching(args[0], COMMANDS.stream()
                    .filter(entry -> visibleTo(sender, entry))
                    .map(CommandHelp::name)
                    .toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("status"))) {
            return matching(args[1], new ArrayList<>(configs.current().worlds().keySet()));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("history")) {
            return matching(args[1], List.of("5", "10", "20", "50"));
        }
        return List.of();
    }

    static List<String> matching(String prefix, List<String> candidates) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private static boolean visibleTo(CommandSender sender, CommandHelp entry) {
        return sender.hasPermission(entry.permission()) && (!entry.playerOnly() || sender instanceof Player);
    }

    private record CommandHelp(String name, String usage, String description, String permission, boolean playerOnly) {}
}
