package com.lozaine.resourceworldresetter.config;

@FunctionalInterface
public interface ConfigChangeListener {
    void onConfigChanged(PluginSettings settings);
}
