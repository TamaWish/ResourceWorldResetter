package com.lozaine.resourceworldresetter.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thin map-backed YAML view used by {@link ConfigRepository} (SnakeYAML, no Bukkit).
 */
final class YamlNode {
    private final Map<String, Object> data;

    YamlNode(Map<String, Object> data) {
        this.data = data == null ? Map.of() : data;
    }

    static YamlNode root(Object loaded) {
        if (loaded instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            return new YamlNode(new LinkedHashMap<>(cast));
        }
        return new YamlNode(new LinkedHashMap<>());
    }

    Map<String, Object> raw() {
        return data;
    }

    boolean contains(String key) {
        return data.containsKey(key) && data.get(key) != null;
    }

    boolean isInt(String key) {
        Object value = data.get(key);
        return value instanceof Integer
                || (value instanceof Long longValue && longValue == longValue.intValue());
    }

    int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    boolean isBoolean(String key) {
        return data.get(key) instanceof Boolean;
    }

    boolean getBoolean(String key) {
        Object value = data.get(key);
        return value instanceof Boolean b && b;
    }

    boolean isString(String key) {
        return data.get(key) instanceof String;
    }

    String getString(String key, String fallback) {
        Object value = data.get(key);
        return value instanceof String s ? s : fallback;
    }

    boolean isList(String key) {
        return data.get(key) instanceof List;
    }

    List<?> getList(String key, List<?> fallback) {
        Object value = data.get(key);
        return value instanceof List<?> list ? list : fallback;
    }

    Object get(String key) {
        return data.get(key);
    }

    void set(String key, Object value) {
        if (key.contains(".")) {
            setPath(key, value);
            return;
        }
        data.put(key, value);
    }

    void setLiteral(String key, Object value) {
        data.put(key, value);
    }

    void createSection(String key) {
        if (!data.containsKey(key) || !(data.get(key) instanceof Map)) {
            data.put(key, new LinkedHashMap<String, Object>());
        }
    }

    YamlNode section(String key) {
        Object value = data.get(key);
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            return new YamlNode(cast);
        }
        return null;
    }

    Set<String> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    private void setPath(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = data;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(parts[i], created);
                current = created;
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) next;
                current = cast;
            }
        }
        current.put(parts[parts.length - 1], value);
    }

    static List<Object> copyList(List<?> source) {
        return new ArrayList<>(source);
    }
}
