package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.RwrApi;
import io.github.tamawish.rwr.api.model.ManagedWorldSnapshot;
import io.github.tamawish.rwr.api.model.ResetStatusSnapshot;
import io.github.tamawish.rwr.config.ConfigService;
import io.github.tamawish.rwr.config.ManagedWorldSettings;
import io.github.tamawish.rwr.config.PluginSettings;
import io.github.tamawish.rwr.reset.ResetCoordinator;
import io.github.tamawish.rwr.reset.ResetStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Internal live implementation of the public read-only API. */
public final class BukkitRwrApi implements RwrApi {
    private final Supplier<PluginSettings> settings;
    private final Function<String, ResetStatus> statuses;

    public BukkitRwrApi(ConfigService configs, ResetCoordinator resets) {
        this(Objects.requireNonNull(configs, "configs")::current, Objects.requireNonNull(resets, "resets")::status);
    }

    BukkitRwrApi(Supplier<PluginSettings> settings, Function<String, ResetStatus> statuses) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.statuses = Objects.requireNonNull(statuses, "statuses");
    }

    @Override
    public List<ManagedWorldSnapshot> managedWorlds() {
        return settings.get().worlds().values().stream().map(ApiMappings::world).toList();
    }

    @Override
    public Optional<ManagedWorldSnapshot> managedWorld(String worldId) {
        return find(worldId).map(ApiMappings::world);
    }

    @Override
    public Optional<ResetStatusSnapshot> resetStatus(String worldId) {
        return find(worldId).map(world -> ApiMappings.status(statuses.apply(world.id())));
    }

    private Optional<ManagedWorldSettings> find(String worldId) {
        Objects.requireNonNull(worldId, "worldId");
        if (worldId.isBlank()) {
            throw new IllegalArgumentException("worldId must not be blank");
        }
        return settings.get().worlds().values().stream()
                .filter(world -> world.id().equalsIgnoreCase(worldId))
                .findFirst();
    }
}
