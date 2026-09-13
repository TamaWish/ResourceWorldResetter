package io.github.tamawish.rwr.reset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.world.WorldProvider;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.Test;

class TypedPlayerEvacuationServiceTest {
  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
  }

  private static Plugin plugin(Server server) {
    return proxy(
        Plugin.class,
        (object, method, args) -> method.getName().equals("getServer") ? server : null);
  }

  private static WorldProvider gateway() {
    return proxy(WorldProvider.class, (object, method, args) -> null);
  }

  private static Messenger messenger() {
    return proxy(Messenger.class, (object, method, args) -> null);
  }

  @Test
  void worldAndPlayerWorkIsQueuedOnOwningSchedulers() {
    AtomicReference<Runnable> worldWork = new AtomicReference<>();
    AtomicReference<Runnable> playerWork = new AtomicReference<>();
    var task =
        proxy(
            io.papermc.paper.threadedregions.scheduler.ScheduledTask.class,
            (object, method, args) -> null);
    var regions =
        proxy(
            io.papermc.paper.threadedregions.scheduler.RegionScheduler.class,
            (object, method, args) -> {
              if (method.getName().equals("run")) {
                @SuppressWarnings("unchecked")
                var callback =
                    (java.util.function.Consumer<
                            io.papermc.paper.threadedregions.scheduler.ScheduledTask>)
                        args[4];
                worldWork.set(() -> callback.accept(task));
              }
              return task;
            });
    var entities =
        proxy(
            io.papermc.paper.threadedregions.scheduler.EntityScheduler.class,
            (object, method, args) -> {
              if (method.getName().equals("run")) {
                @SuppressWarnings("unchecked")
                var callback =
                    (java.util.function.Consumer<
                            io.papermc.paper.threadedregions.scheduler.ScheduledTask>)
                        args[1];
                playerWork.set(() -> callback.accept(task));
              }
              return task;
            });
    Server server =
        proxy(
            Server.class,
            (object, method, args) ->
                switch (method.getName()) {
                  case "getMessenger" -> messenger();
                  case "getRegionScheduler" -> regions;
                  default -> null;
                });
    Player player =
        proxy(
            Player.class,
            (object, method, args) -> method.getName().equals("getScheduler") ? entities : null);
    World world = proxy(World.class, (object, method, args) -> null);
    TypedPlayerEvacuationService service =
        new TypedPlayerEvacuationService(plugin(server), gateway());
    AtomicInteger calls = new AtomicInteger();
    service.onWorld(world, calls::incrementAndGet);
    service.onPlayer(
        player,
        calls::incrementAndGet,
        () -> {
          throw new AssertionError("Unexpected retirement");
        });
    assertThat(calls).hasValue(0);
    worldWork.get().run();
    playerWork.get().run();
    assertThat(calls).hasValue(2);
  }

  @Test
  void teleportUsesAsyncApiWithoutWaiting() {
    Server server =
        proxy(
            Server.class,
            (object, method, args) -> method.getName().equals("getMessenger") ? messenger() : null);
    CompletableFuture<Boolean> completion = new CompletableFuture<>();
    Player player =
        proxy(
            Player.class,
            (object, method, args) -> {
              assertThat(method.getName()).isEqualTo("teleportAsync");
              return completion;
            });
    TypedPlayerEvacuationService service =
        new TypedPlayerEvacuationService(plugin(server), gateway());
    assertThat(service.teleport(player, new Location(null, 0, 80, 0))).isSameAs(completion);
    assertThat(completion).isNotDone();
  }
}
