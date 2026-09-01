package io.github.tamawish.rwr.bukkitapi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.tamawish.rwr.api.RwrApi;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;

class RwrApiRegistrationTest {
    @Test
    void registersAndUnregistersTheExactServiceOnce() {
        Server server = mock(Server.class);
        ServicesManager services = mock(ServicesManager.class);
        Plugin plugin = mock(Plugin.class);
        RwrApi api = mock(RwrApi.class);
        when(server.getServicesManager()).thenReturn(services);

        RwrApiRegistration registration = RwrApiRegistration.register(server, plugin, api);
        verify(services).register(RwrApi.class, api, plugin, ServicePriority.Normal);
        registration.close();
        registration.close();
        verify(services, times(1)).unregister(RwrApi.class, api);
    }
}
