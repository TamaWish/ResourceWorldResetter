package com.lozaine.resourceworldresetter.config;

import java.io.IOException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ConfigService {
    private final ConfigRepository repository;
    private final AtomicReference<PluginSettings> active = new AtomicReference<>();
    private final CopyOnWriteArrayList<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

    public ConfigService(ConfigRepository repository) {
        this.repository = repository;
    }

    public ReloadResult reload() {
        ConfigLoadResult loaded = repository.load();
        if (!loaded.valid()) {
            return new ReloadResult(false, active.get() != null, loaded.status(), loaded.issues());
        }
        active.set(loaded.settings());
        notifyListeners(loaded.settings());
        return new ReloadResult(true, false, ConfigLoadStatus.VALID, List.of());
    }

    public ReloadResult saveAndApply(PluginSettings candidate) {
        try {
            repository.save(candidate);
        } catch (IOException exception) {
            return new ReloadResult(
                    false,
                    active.get() != null,
                    ConfigLoadStatus.INVALID,
                    List.of(new ConfigIssue("config.yml", "atomic save failed: " + exception.getMessage())));
        } catch (ConfigValidationException exception) {
            return new ReloadResult(false, active.get() != null, ConfigLoadStatus.INVALID, exception.issues());
        }
        active.set(candidate);
        notifyListeners(candidate);
        return new ReloadResult(true, false, ConfigLoadStatus.VALID, List.of());
    }

    public PluginSettings current() {
        return Optional.ofNullable(active.get())
                .orElseThrow(() -> new IllegalStateException("No valid configuration is active"));
    }

    public ListenerRegistration addChangeListener(ConfigChangeListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public ReconciliationResult reconcileWorldStates(WorldCatalogView catalog) {
        while (true) {
            PluginSettings settings = active.get();
            if (settings == null) {
                return new ReconciliationResult(0);
            }
            int changed = 0;
            Map<String, ManagedWorldSettings> reconciled = new LinkedHashMap<>();
            for (Map.Entry<String, ManagedWorldSettings> entry : settings.worlds().entrySet()) {
                ManagedWorldSettings world = entry.getValue();
                WorldOperationalState state = WorldStateResolver.resolve(
                        world.multiverseWorld(),
                        world.enabled(),
                        world.managed(),
                        settings.defaultHubWorld(),
                        catalog);
                if (state != world.state()) {
                    changed++;
                }
                reconciled.put(entry.getKey(), world.withState(state));
            }
            if (changed == 0) {
                return new ReconciliationResult(0);
            }
            PluginSettings updated = new PluginSettings(
                    settings.configVersion(),
                    settings.timezone(),
                    settings.defaultHubWorld(),
                    settings.resetPolicy(),
                    reconciled,
                    settings.teleport());
            if (active.compareAndSet(settings, updated)) {
                return new ReconciliationResult(changed);
            }
        }
    }

    public record ReloadResult(
            boolean accepted,
            boolean retainedPrevious,
            ConfigLoadStatus status,
            List<ConfigIssue> issues) {
        public ReloadResult {
            issues = List.copyOf(issues);
        }
    }

    public record ReconciliationResult(int changedWorlds) {}

    private void notifyListeners(PluginSettings settings) {
        listeners.forEach(listener -> listener.onConfigChanged(settings));
    }
}
