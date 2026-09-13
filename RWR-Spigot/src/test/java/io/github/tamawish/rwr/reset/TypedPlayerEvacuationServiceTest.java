package io.github.tamawish.rwr.reset;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.world.WorldProvider;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Location;
import org.bukkit.Server;
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
  void offThreadWorldAndPlayerWorkIsQueuedOnPrimaryScheduler() {
    java.util.List<Runnable> queue = new java.util.ArrayList<>();
    var scheduler =
        proxy(
            org.bukkit.scheduler.BukkitScheduler.class,
            (object, method, args) -> {
              if (method.getName().equals("runTask")) queue.add((Runnable) args[1]);
              return null;
            });
    Server server =
        proxy(
            Server.class,
            (object, method, args) ->
                switch (method.getName()) {
                  case "getMessenger" -> messenger();
                  case "isPrimaryThread" -> false;
                  case "getScheduler" -> scheduler;
                  default -> null;
                });
    TypedPlayerEvacuationService service =
        new TypedPlayerEvacuationService(plugin(server), gateway());
    AtomicInteger calls = new AtomicInteger();
    service.onWorld(null, calls::incrementAndGet);
    service.onPlayer(null, calls::incrementAndGet, () -> {});
    assertThat(calls).hasValue(0);
    queue.forEach(Runnable::run);
    assertThat(calls).hasValue(2);
  }

  @Test
  void primaryThreadWorkRunsDirectlyAndTeleportReturnsResult() {
    Server server =
        proxy(
            Server.class,
            (object, method, args) ->
                switch (method.getName()) {
                  case "getMessenger" -> messenger();
                  case "isPrimaryThread" -> true;
                  default -> null;
                });
    Player player =
        proxy(
            Player.class,
            (object, method, args) -> {
              assertThat(method.getName()).isEqualTo("teleport");
              return true;
            });
    TypedPlayerEvacuationService service =
        new TypedPlayerEvacuationService(plugin(server), gateway());
    AtomicInteger calls = new AtomicInteger();
    service.onWorld(null, calls::incrementAndGet);
    assertThat(calls).hasValue(1);
    assertThat(service.teleport(player, new Location(null, 0, 80, 0)).toCompletableFuture().join())
        .isTrue();
  }
}
