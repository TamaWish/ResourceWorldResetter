package io.github.tamawish.rwr.worlds;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.multiverse.RegenerationOutcome;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.thenextlvl.worlds.WorldsAccess;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

class WorldsWorldProviderTest {
  @Test
  void restorationWaitsForGlobalRegionAndReportsSuccessOnlyAfterSettingsAreApplied() {
    verifyRestoration(false);
  }

  @Test
  void restorationFailureRetainsItsDiagnostic() {
    verifyRestoration(true);
  }

  @SuppressWarnings("unchecked")
  private void verifyRestoration(boolean failRestore) {
    AtomicBoolean global = new AtomicBoolean();
    AtomicBoolean restored = new AtomicBoolean();
    AtomicInteger borderWrites = new AtomicInteger();
    WorldBorder border =
        proxy(
            WorldBorder.class,
            (p, m, a) -> {
              assertThat(global).isTrue();
              borderWrites.incrementAndGet();
              return null;
            });
    AtomicReference<Consumer<ScheduledTask>> queued = new AtomicReference<>();
    GlobalRegionScheduler scheduler =
        proxy(
            GlobalRegionScheduler.class,
            (p, m, a) -> {
              if (m.getName().equals("run")) {
                queued.set((Consumer<ScheduledTask>) a[1]);
              }
              return null;
            });
    Server server =
        proxy(
            Server.class,
            (p, m, a) -> {
              if (m.getName().equals("getRegionScheduler")) {
                throw new AssertionError("Restoration must not use a world region");
              }
              return m.getName().equals("getGlobalRegionScheduler") ? scheduler : null;
            });
    Plugin plugin =
        proxy(
            Plugin.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "getServer" -> server;
                  case "getLogger" -> Logger.getLogger("WorldsWorldProviderTest");
                  default -> null;
                });
    World world =
        proxy(
            World.class,
            (p, m, a) ->
                switch (m.getName()) {
                  case "setGameRule" -> {
                    assertThat(global).isTrue();
                    assertThat(a).containsExactly(GameRule.KEEP_INVENTORY, true);
                    if (failRestore) {
                      throw new IllegalStateException("restore failed");
                    }
                    restored.set(true);
                    yield true;
                  }
                  case "getName" -> "worlds_kreat";
                  case "getWorldBorder" -> border;
                  case "getSpawnLocation" -> new Location((World) p, 0, 64, 0);
                  case "getEnvironment" -> World.Environment.NORMAL;
                  case "getSeed" -> 1L;
                  case "canGenerateStructures" -> true;
                  default -> null;
                });
    WorldsWorldProvider provider =
        new WorldsWorldProvider(plugin, proxy(WorldsAccess.class, (p, m, a) -> null));
    CompletableFuture<RegenerationOutcome> result = new CompletableFuture<>();
    provider.restoreOnGlobalRegion(
        "worlds:kreat",
        world,
        new WorldsWorldProvider.PreservedWorldState(
            Map.of(GameRule.KEEP_INVENTORY, true),
            new WorldsWorldProvider.BorderState(0, 0, 1000, 0.2, 5, 5, 15)),
        result);

    assertThat(result).isNotDone();
    assertThat(restored).isFalse();
    global.set(true);
    queued.get().accept(null);
    if (failRestore) {
      assertThat(result.join())
          .isInstanceOfSatisfying(
              RegenerationOutcome.Failed.class,
              failure ->
                  assertThat(failure.toString())
                      .contains("STATE_RESTORE_FAILED", "restore failed"));
    } else {
      assertThat(restored).isTrue();
      assertThat(borderWrites).hasValue(6);
      assertThat(result.join()).isInstanceOf(RegenerationOutcome.Success.class);
    }
  }

  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
  }
}
