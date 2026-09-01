package io.github.tamawish.rwr.message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper-native Adventure message service. Uses {@link CommandSender#sendMessage(Component)}
 * directly — no adventure-platform-bukkit shade.
 */
public final class MessageService {
    private static final Pattern PERCENT_PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");
    private static final Map<Character, String> AMPERSAND_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> templates = new LinkedHashMap<>();
    private String prefixTemplate = "<gradient:#00C9FF:#92FE9D>[RWR]</gradient> ";

    public MessageService(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration messages = YamlConfiguration.loadConfiguration(file);
        applyBundledDefaults(plugin, messages);
        loadTemplates(messages);
    }

    public void close() {
        // No audience bridge to close on Paper.
    }

    private void applyBundledDefaults(JavaPlugin plugin, YamlConfiguration messages) {
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream == null) {
                plugin.getLogger().warning("Bundled messages.yml is missing; message fallbacks are unavailable.");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
                messages.options().copyDefaults(true);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not load bundled message defaults: " + exception.getMessage());
        }
    }

    private void loadTemplates(YamlConfiguration messages) {
        templates.clear();
        ConfigurationSection defaults = messages.getDefaults();
        if (defaults != null) {
            flatten("", defaults, templates);
        }
        Map<String, String> overrides = new LinkedHashMap<>();
        flatten("", messages, overrides);
        templates.putAll(overrides);
        String rawPrefix = templates.getOrDefault("prefix", prefixTemplate);
        prefixTemplate = normalizeTemplate(rawPrefix);
        templates.replaceAll((key, value) -> normalizeTemplate(value));
        templates.put("prefix", prefixTemplate);
    }

    private static void flatten(String path, ConfigurationSection section, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            String full = path.isEmpty() ? key : path + '.' + key;
            ConfigurationSection child = section.getConfigurationSection(key);
            if (child != null) {
                flatten(full, child, out);
            } else {
                String value = section.getString(key);
                if (value != null) {
                    out.put(full, value);
                }
            }
        }
    }

    public void send(CommandSender target, String key, Object... placeholders) {
        target.sendMessage(component(key, placeholders));
    }

    public void broadcast(Server server, String key, Object... placeholders) {
        server.sendMessage(component(key, placeholders));
    }

    public Component component(String key, Object... placeholders) {
        String template = templates.getOrDefault(key, key);
        return miniMessage.deserialize(prepareForDeserialize(template), resolvers(placeholders));
    }

    public String text(String key, Object... placeholders) {
        return LegacyComponentSerializer.legacySection().serialize(component(key, placeholders));
    }

    public String plain(String key, Object... placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, placeholders));
    }

    private TagResolver resolvers(Object... placeholders) {
        TagResolver.Builder builder = TagResolver.builder()
                .resolver(Placeholder.parsed("prefix", prefixTemplate));
        Map<String, String> pairs = pairs(placeholders);
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if ("prefix".equals(entry.getKey())) {
                continue;
            }
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    static String normalizeTemplate(String raw) {
        if (raw == null) {
            return "";
        }
        return ampersandToMiniMessage(percentToMiniMessage(raw));
    }

    static String percentToMiniMessage(String raw) {
        Matcher matcher = PERCENT_PLACEHOLDER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(out, Matcher.quoteReplacement('<' + matcher.group(1) + '>'));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    static String ampersandToMiniMessage(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        for (int index = 0; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (current == '&' && index + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(index + 1));
                String tag = AMPERSAND_TAGS.get(code);
                if (tag != null) {
                    out.append(tag);
                    index++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String prepareForDeserialize(String template) {
        if (template.indexOf('&') < 0) {
            return template;
        }
        return ampersandToMiniMessage(template);
    }

    private static Map<String, String> pairs(Object... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Message placeholders must be key/value pairs");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), String.valueOf(values[index + 1]));
        }
        return result;
    }
}
