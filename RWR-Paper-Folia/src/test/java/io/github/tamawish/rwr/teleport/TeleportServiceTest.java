package io.github.tamawish.rwr.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.config.TeleportDestinationSettings;
import io.github.tamawish.rwr.config.TeleportSettings;
import io.github.tamawish.rwr.multiverse.DestinationResult;
import io.github.tamawish.rwr.multiverse.RegenerationOutcome;
import io.github.tamawish.rwr.multiverse.RegenerationRequest;
import io.github.tamawish.rwr.multiverse.WorldSnapshot;
import io.github.tamawish.rwr.reset.ResetAccessPolicy;
import io.github.tamawish.rwr.world.SafeLocation;
import io.github.tamawish.rwr.world.WorldProvider;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class TeleportServiceTest {
  @Test
  void permitRemainsHeldAcrossAsyncDestinationResolutionAndTeleport() throws Exception {
    AsyncGateway gateway = new AsyncGateway();
    AtomicBoolean permitHeld = new AtomicBoolean();
    AtomicInteger permitReleases = new AtomicInteger();
    ResetAccessPolicy access =
        new ResetAccessPolicy() {
          @Override
          public boolean blocksIncomingRwrTeleport(String world) {
            return false;
          }

          @Override
          public Optional<TeleportPermit> tryAcquireIncomingRwrTeleport(String world) {
            permitHeld.set(true);
            return Optional.of(
                () -> {
                  permitHeld.set(false);
                  permitReleases.incrementAndGet();
                });
          }
        };
    TeleportSettings settings =
        new TeleportSettings(
            false, false, true, Map.of("resource", new TeleportDestinationSettings(true, null)));
    TeleportService service =
        new TeleportService(() -> settings, gateway, access, name -> name, (key, values) -> key);
    CompletableFuture<Boolean> teleport = new CompletableFuture<>();

    CompletionStage<TeleportAttempt> attempt =
        service.teleport(player(permitHeld, teleport), "resource");

    assertThat(attempt.toCompletableFuture()).isNotDone();
    assertThat(permitHeld).isTrue();
    gateway.destination.complete(
        new DestinationResult.Available(new SafeLocation("resource", 0, 64, 0), false));
    assertThat(permitHeld).isTrue();
    teleport.complete(true);

    assertThat(attempt.toCompletableFuture().get(5, TimeUnit.SECONDS).successful()).isTrue();
    assertThat(permitHeld).isFalse();
    assertThat(permitReleases).hasValue(1);
  }

  private static Player player(
      AtomicBoolean permitHeld, CompletableFuture<Boolean> teleportResult) {
    World world =
        (World)
            Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, arguments) ->
                    method.getName().equals("getName")
                        ? "resource"
                        : defaultValue(method.getReturnType()));
    Server server =
        (Server)
            Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, arguments) ->
                    method.getName().equals("getWorld")
                        ? world
                        : defaultValue(method.getReturnType()));
    return (Player)
        Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, arguments) -> {
              if (method.getName().equals("hasPermission")) {
                return true;
              }
              if (method.getName().equals("getServer")) {
                return server;
              }
              if (method.getName().equals("teleportAsync")) {
                if (!permitHeld.get()) {
                  throw new AssertionError("teleport ran without its admission permit");
                }
                return teleportResult;
              }
              return defaultValue(method.getReturnType());
            });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == char.class) {
      return '\0';
    }
    return 0;
  }

  private static final class AsyncGateway implements WorldProvider {
    private final WorldSnapshot world =
        new WorldSnapshot(
            "resource",
            "resource",
            "Resource",
            true,
            "NORMAL",
            1L,
            "",
            "",
            "NORMAL",
            true,
            true,
            "resource 0 64 0");
    private final CompletableFuture<DestinationResult> destination = new CompletableFuture<>();

    @Override
    public List<WorldSnapshot> registeredWorlds() {
      return List.of(world);
    }

    @Override
    public List<WorldSnapshot> loadedWorlds() {
      return List.of(world);
    }

    @Override
    public Optional<WorldSnapshot> world(String name) {
      return Optional.of(world);
    }

    @Override
    public DestinationResult resolveSafeDestination(String name) {
      throw new AssertionError("synchronous destination resolution must not be used");
    }

    @Override
    public CompletionStage<DestinationResult> resolveSafeDestinationAsync(String name) {
      return destination;
    }

    @Override
    public RegenerationOutcome regenerate(RegenerationRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> registeredWorldNames() {
      return Set.of("resource");
    }

    @Override
    public String defaultWorldName() {
      return "world";
    }
  }
}
