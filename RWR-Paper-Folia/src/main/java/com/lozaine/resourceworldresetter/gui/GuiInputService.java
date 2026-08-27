package com.lozaine.resourceworldresetter.gui;

import com.lozaine.resourceworldresetter.message.MessageService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Anvil text input with chat fallback, scheduled via Folia global/async schedulers. */
public final class GuiInputService implements Listener, AutoCloseable {
    private static final long TIMEOUT_TICKS = Duration.ofSeconds(30).toSeconds() * 20L;
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, PendingChat> pending = new ConcurrentHashMap<>();

    public GuiInputService(JavaPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void request(Player player, String title, String initial, Consumer<String> callback) {
        cancel(player.getUniqueId());
        try {
            new AnvilGUI.Builder()
                    .plugin(plugin)
                    .title(title)
                    .text(initial.isBlank() ? " " : initial)
                    .itemLeft(new ItemStack(Material.PAPER))
                    .onClick((slot, state) -> {
                        if (slot != AnvilGUI.Slot.OUTPUT) {
                            return List.of();
                        }
                        String value = state.getText().trim();
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, ignored -> callback.accept(value));
                        return List.of(AnvilGUI.ResponseAction.close());
                    })
                    .open(player);
        } catch (LinkageError | RuntimeException unavailable) {
            openChat(player, callback);
        }
    }

    private void openChat(Player player, Consumer<String> callback) {
        player.closeInventory();
        messages.send(player, "gui.input-prompt");
        ScheduledTask timeout = plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, ignored -> {
            PendingChat removed = pending.remove(player.getUniqueId());
            if (removed != null) {
                messages.send(player, "gui.input-timeout");
            }
        }, TIMEOUT_TICKS);
        pending.put(player.getUniqueId(), new PendingChat(callback, timeout));
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        PendingChat value = pending.remove(event.getPlayer().getUniqueId());
        if (value == null) {
            return;
        }
        event.setCancelled(true);
        value.timeout().cancel();
        String message = event.getMessage().trim();
        plugin.getServer().getGlobalRegionScheduler().run(plugin, ignored -> {
            if (message.equalsIgnoreCase("cancel")) {
                messages.send(event.getPlayer(), "gui.input-cancelled");
            } else {
                value.callback().accept(message);
            }
        });
    }

    private void cancel(UUID playerId) {
        PendingChat old = pending.remove(playerId);
        if (old != null) {
            old.timeout().cancel();
        }
    }

    @Override
    public void close() {
        pending.values().forEach(value -> value.timeout().cancel());
        pending.clear();
    }

    private record PendingChat(Consumer<String> callback, ScheduledTask timeout) {}
}
