package com.lozaine.resourceworldresetter.teleport;

import static org.assertj.core.api.Assertions.assertThat;

import com.lozaine.resourceworldresetter.config.TeleportDestinationSettings;
import com.lozaine.resourceworldresetter.config.TeleportSettings;
import com.lozaine.resourceworldresetter.multiverse.DestinationResult;
import com.lozaine.resourceworldresetter.multiverse.RegenerationOutcome;
import com.lozaine.resourceworldresetter.multiverse.RegenerationRequest;
import com.lozaine.resourceworldresetter.multiverse.WorldSnapshot;
import com.lozaine.resourceworldresetter.world.SafeLocation;
import com.lozaine.resourceworldresetter.world.WorldProvider;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class TeleportServiceTest {
    @Test
    void publicDestinationUsesOneSafeResolutionAndOneSynchronousTeleport() {
        AtomicBoolean resetting = new AtomicBoolean();
        FakeGateway gateway = new FakeGateway(resetting, false);
        TeleportService service = service(gateway, resetting, null);
        AtomicInteger teleportCalls = new AtomicInteger();
        Player player = player(Set.of(), teleportCalls);

        TeleportAttempt result = service.teleport(player, "resource");

        assertThat(result.successful()).isTrue();
        assertThat(gateway.resolveCalls).isEqualTo(1);
        assertThat(teleportCalls).hasValue(1);
    }

    @Test
    void permissionIsRevalidatedBeforeSafeDestinationResolution() {
        AtomicBoolean resetting = new AtomicBoolean();
        FakeGateway gateway = new FakeGateway(resetting, false);
        TeleportService service = service(gateway, resetting, "rank.vip");
        AtomicInteger teleportCalls = new AtomicInteger();

        TeleportAttempt result = service.teleport(player(Set.of(), teleportCalls), "resource");

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).contains("permission");
        assertThat(gateway.resolveCalls).isZero();
        assertThat(teleportCalls).hasValue(0);
    }

    @Test
    void resetStartingDuringSafeResolutionCancelsFinalTeleport() {
        AtomicBoolean resetting = new AtomicBoolean();
        FakeGateway gateway = new FakeGateway(resetting, true);
        TeleportService service = service(gateway, resetting, null);
        AtomicInteger teleportCalls = new AtomicInteger();

        TeleportAttempt result = service.teleport(player(Set.of(), teleportCalls), "resource");

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).contains("started resetting");
        assertThat(gateway.resolveCalls).isEqualTo(1);
        assertThat(teleportCalls).hasValue(0);
    }

    private static TeleportService service(
            FakeGateway gateway, AtomicBoolean resetting, String permission) {
        TeleportSettings settings = new TeleportSettings(
                false,
                false,
                true,
                Map.of("resource", new TeleportDestinationSettings(true, permission)));
        return new TeleportService(() -> settings, gateway, world -> resetting.get());
    }

    private static Player player(Set<String> permissions, AtomicInteger teleportCalls) {
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] {World.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getName")) {
                        return "resource";
                    }
                    return defaultValue(method.getReturnType());
                });
        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[] {Server.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getWorld")
                            && arguments != null
                            && arguments.length == 1) {
                        return world;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("hasPermission")
                            && arguments != null
                            && arguments.length == 1
                            && arguments[0] instanceof String permission) {
                        return permissions.contains(permission);
                    }
                    if (method.getName().equals("getServer")) {
                        return server;
                    }
                    if (method.getName().equals("teleport")
                            && arguments != null
                            && arguments.length == 2) {
                        teleportCalls.incrementAndGet();
                        return true;
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

    private static final class FakeGateway implements WorldProvider {
        private final AtomicBoolean resetting;
        private final boolean startResetDuringResolve;
        private final WorldSnapshot world = new WorldSnapshot(
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
        private int resolveCalls;

        private FakeGateway(AtomicBoolean resetting, boolean startResetDuringResolve) {
            this.resetting = resetting;
            this.startResetDuringResolve = startResetDuringResolve;
        }

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
            return name.equalsIgnoreCase(world.name()) ? Optional.of(world) : Optional.empty();
        }

        @Override
        public DestinationResult resolveSafeDestination(String name) {
            resolveCalls++;
            if (startResetDuringResolve) {
                resetting.set(true);
            }
            return new DestinationResult.Available(new SafeLocation(name, 0, 64, 0), false);
        }

        @Override
        public RegenerationOutcome regenerate(RegenerationRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> registeredWorldNames() {
            return Set.of(world.name());
        }

        @Override
        public String defaultWorldName() {
            return world.name();
        }
    }
}
