package io.github.tamawish.rwr.bukkitapi;

import io.github.tamawish.rwr.api.RwrApi;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

/** Owns the exact Bukkit services-manager registration for the RWR API. */
public final class RwrApiRegistration implements AutoCloseable {
    private final ServicesManager services;
    private final RwrApi api;
    private final AtomicBoolean open = new AtomicBoolean(true);

    private RwrApiRegistration(ServicesManager services, RwrApi api) {
        this.services = services;
        this.api = api;
    }

    public static RwrApiRegistration register(Server server, Plugin plugin, RwrApi api) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(api, "api");
        ServicesManager services = server.getServicesManager();
        services.register(RwrApi.class, api, plugin, ServicePriority.Normal);
        return new RwrApiRegistration(services, api);
    }

    @Override
    public void close() {
        if (open.compareAndSet(true, false)) {
            services.unregister(RwrApi.class, api);
        }
    }
}
