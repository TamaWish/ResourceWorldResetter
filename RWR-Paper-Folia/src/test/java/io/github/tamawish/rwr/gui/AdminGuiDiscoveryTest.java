package io.github.tamawish.rwr.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.tamawish.rwr.bootstrap.UpdateService;
import io.github.tamawish.rwr.bukkitapi.DestinationCatalog;
import io.github.tamawish.rwr.config.*;
import io.github.tamawish.rwr.message.MessageService;
import io.github.tamawish.rwr.reset.ResetCoordinator;
import io.github.tamawish.rwr.scheduler.ScheduleManager;
import io.github.tamawish.rwr.world.WorldProvider;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class AdminGuiDiscoveryTest {
  private final JavaPlugin plugin = mock(JavaPlugin.class);
  private final Player player = mock(Player.class);
  private final InventoryView view = mock(InventoryView.class);
  private final ConfigService configs = mock(ConfigService.class);
  private final PluginSettings settings = mock(PluginSettings.class);
  private final DestinationCatalog catalog = mock(DestinationCatalog.class);
  private final List<Runnable> scheduled = new ArrayList<>();
  private final CompletableFuture<Boolean> response = new CompletableFuture<>();
  private MockedStatic<Bukkit> bukkit;
  private MockedConstruction<ItemStack> items;
  private AdminGuiService gui;
  private Inventory open;

  @BeforeEach
  void setup() {
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    when(plugin.isEnabled()).thenReturn(true);
    EntityScheduler scheduler = mock(EntityScheduler.class);
    when(player.getScheduler()).thenReturn(scheduler);
    doAnswer(
            call -> {
              Consumer<ScheduledTask> task = call.getArgument(1);
              scheduled.add(() -> task.accept(null));
              return null;
            })
        .when(scheduler)
        .run(eq(plugin), any(), isNull());
    when(player.isOnline()).thenReturn(true);
    when(player.hasPermission("rwr.admin")).thenReturn(true);
    when(player.getOpenInventory()).thenReturn(view);
    open = mock(Inventory.class);
    when(view.getTopInventory()).thenAnswer(ignored -> open);
    doAnswer(
            call -> {
              open = call.getArgument(0);
              return view;
            })
        .when(player)
        .openInventory(any(Inventory.class));
    when(configs.current()).thenReturn(settings);
    when(settings.defaultEvacuation())
        .thenReturn(
            new EvacuationSettings(
                true,
                new EvacuationDestination(EvacuationDestinationType.PROXY_SERVER, "server0"),
                30));
    when(settings.worlds()).thenReturn(Map.of());
    when(catalog.discover(player)).thenReturn(response);
    when(catalog.entries(eq(settings), any(), any())).thenReturn(entries(60, true));
    MessageService messages = mock(MessageService.class);
    when(messages.component(anyString())).thenReturn(Component.text("Destinations"));
    when(messages.plain(anyString())).thenAnswer(call -> call.getArgument(0));
    items =
        mockConstruction(
            ItemStack.class,
            (item, context) -> {
              when(item.getItemMeta()).thenReturn(mock(ItemMeta.class));
            });
    bukkit = mockStatic(Bukkit.class);
    bukkit
        .when(
            () ->
                Bukkit.createInventory(any(InventoryHolder.class), anyInt(), any(Component.class)))
        .thenAnswer(
            call -> {
              Inventory inventory = mock(Inventory.class);
              InventoryHolder holder = call.getArgument(0);
              when(inventory.getHolder()).thenReturn(holder);
              when(inventory.getSize()).thenReturn(54);
              return inventory;
            });
    gui =
        new AdminGuiService(
            plugin,
            configs,
            mock(WorldProvider.class),
            mock(ResetCoordinator.class),
            mock(ScheduleManager.class),
            mock(GuiInputService.class),
            messages,
            mock(UpdateService.class),
            catalog);
  }

  @AfterEach
  void cleanup() {
    bukkit.close();
    items.close();
  }

  @Test
  void opensImmediatelyAndRefreshKeepsCurrentPageAndRequestIdentity() throws Exception {
    show(0);
    assertThat(field("discovering")).isEqualTo(true);
    Object request = field("requestId");
    show(1);
    assertThat(field("page")).isEqualTo(1);
    verify(catalog).discover(player);
    Inventory before = open;
    response.complete(true);
    assertThat(open).isSameAs(before);
    drain();
    assertThat(open).isNotSameAs(before);
    assertThat(field("page")).isEqualTo(1);
    assertThat(field("requestId")).isEqualTo(request);
    assertThat(field("discovering")).isEqualTo(false);
  }

  @Test
  void completionCannotReopenClosedOrDifferentInventory() throws Exception {
    show(0);
    Inventory other = mock(Inventory.class);
    open = other;
    response.complete(true);
    drain();
    assertThat(open).isSameAs(other);
  }

  @Test
  void oldResponseCannotOverwriteNewSelector() throws Exception {
    show(0);
    open = mock(Inventory.class);
    when(catalog.discover(player)).thenReturn(new CompletableFuture<>());
    show(0);
    Inventory newer = open;
    response.complete(true);
    drain();
    assertThat(open).isSameAs(newer);
    assertThat(field("discovering")).isEqualTo(true);
  }

  @Test
  void unavailableCurrentTargetHasNoClickActionAndManualAndBackRemain() throws Exception {
    when(catalog.entries(eq(settings), any(), any())).thenReturn(entries(1, false));
    show(0);
    assertThat(actions()).doesNotContainKey(10).containsKeys(45, 49);
    response.complete(false);
    drain();
    assertThat(field("discoveryFailed")).isEqualTo(true);
    assertThat(actions()).doesNotContainKey(10).containsKeys(45, 49);
  }

  @Test
  void clickDefersUntilOwningSchedulerAndRechecksAvailability() throws Exception {
    show(0);
    when(catalog.entries(eq(settings), any(), any())).thenReturn(entries(60, false));
    InventoryClickEvent click = mock(InventoryClickEvent.class);
    when(click.getView()).thenReturn(view);
    when(click.getWhoClicked()).thenReturn(player);
    when(click.getRawSlot()).thenReturn(10);
    Inventory before = open;
    gui.onClick(click);
    verify(click).setCancelled(true);
    assertThat(open).isSameAs(before);
    drain();
    assertThat(open).isNotSameAs(before);
    assertThat(actions()).doesNotContainKey(10);
    verify(configs, never()).saveAndApply(any());
  }

  private void show(int page) throws Exception {
    Method method =
        AdminGuiService.class.getDeclaredMethod(
            "openEvacuationList", Player.class, String.class, int.class);
    method.setAccessible(true);
    method.invoke(gui, player, "\nproxy-server", page);
  }

  private Object field(String name) throws Exception {
    Field field = open.getHolder().getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(open.getHolder());
  }

  @SuppressWarnings("unchecked")
  private Map<Integer, ?> actions() throws Exception {
    Field field = open.getHolder().getClass().getSuperclass().getDeclaredField("actions");
    field.setAccessible(true);
    return (Map<Integer, ?>) field.get(open.getHolder());
  }

  private void drain() {
    List<Runnable> tasks = List.copyOf(scheduled);
    scheduled.clear();
    tasks.forEach(Runnable::run);
  }

  private static List<DestinationCatalog.Entry> entries(int count, boolean available) {
    return IntStream.range(0, count)
        .mapToObj(index -> new DestinationCatalog.Entry("server" + index, available, true))
        .toList();
  }
}
