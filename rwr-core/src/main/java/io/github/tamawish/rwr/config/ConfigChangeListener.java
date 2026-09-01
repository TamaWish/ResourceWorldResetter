package io.github.tamawish.rwr.config;

@FunctionalInterface
public interface ConfigChangeListener {
    void onConfigChanged(PluginSettings settings);
}
