package io.github.tamawish.rwr.message;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
  private static final Map<Character, String> AMPERSAND_TAGS =
      Map.ofEntries(
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
  private final Set<String> reportedMissingKeys = new HashSet<>();
  private final JavaPlugin plugin;
  private String prefixTemplate = "<gradient:#00C9FF:#92FE9D>[RWR]</gradient> ";

  /**
   * Creates a message service and loads its initial locale.
   *
   * @param plugin owning plugin
   * @throws IllegalStateException if the bundled fallback locale cannot be loaded
   */
  public MessageService(JavaPlugin plugin) {
    this.plugin = plugin;
    if (!reload()) {
      throw new IllegalStateException("Bundled en_US locale could not be loaded");
    }
  }

  public void close() {
    // No audience bridge to close on Paper.
  }

  /**
   * Atomically reloads and validates the configured locale.
   *
   * @return whether the candidate locale was activated
   */
  public synchronized boolean reload() {
    String locale =
        YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"))
            .getString("locale", "en_US");
    String localeResource = "locales/" + locale + ".yml";
    File localeFile = new File(plugin.getDataFolder(), localeResource);
    File englishFile = new File(plugin.getDataFolder(), "locales/en_US.yml");
    if (!englishFile.exists()) {
      plugin.saveResource("locales/en_US.yml", false);
    }
    if (!localeFile.exists()) {
      try (InputStream bundledLocale = plugin.getResource(localeResource)) {
        if (bundledLocale != null) {
          plugin.saveResource(localeResource, false);
        }
      } catch (IOException exception) {
        plugin.getLogger().warning("Could not inspect bundled locale " + locale + '.');
      }
    }
    try {
      Map<String, String> candidate = new LinkedHashMap<>();
      try (InputStream stream = plugin.getResource("locales/en_US.yml")) {
        if (stream == null) {
          throw new IOException("Bundled en_US locale is missing");
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
          YamlConfiguration bundled = new YamlConfiguration();
          bundled.load(reader);
          flatten("", bundled, candidate);
        }
      }
      YamlConfiguration english = new YamlConfiguration();
      english.load(englishFile);
      flatten("", english, candidate);
      if (!locale.equals("en_US")) {
        try (InputStream stream = plugin.getResource(localeResource)) {
          if (stream != null) {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
              YamlConfiguration bundledSelected = new YamlConfiguration();
              bundledSelected.load(reader);
              flatten("", bundledSelected, candidate);
            }
          }
        }
        if (!localeFile.exists()) {
          plugin.getLogger().warning("Locale " + locale + " was not found; using en_US.");
        } else {
          YamlConfiguration selected = new YamlConfiguration();
          selected.load(localeFile);
          flatten("", selected, candidate);
        }
      }
      String candidatePrefix =
          normalizeTemplate(
              candidate.getOrDefault("prefix", "<gradient:#00C9FF:#92FE9D>[RWR]</gradient> "));
      candidate.replaceAll((key, value) -> normalizeTemplate(value));
      candidate.put("prefix", candidatePrefix);
      candidate.forEach(
          (key, value) ->
              miniMessage.deserialize(
                  prepareForDeserialize(value), Placeholder.parsed("prefix", candidatePrefix)));
      templates.clear();
      templates.putAll(candidate);
      prefixTemplate = candidatePrefix;
      reportedMissingKeys.clear();
      return true;
    } catch (Exception exception) {
      plugin
          .getLogger()
          .warning(
              "Locale reload rejected; previous messages remain active: " + exception.getMessage());
      return false;
    }
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

  /**
   * Renders a localized Adventure component with escaped placeholder values.
   *
   * @param key locale message key
   * @param placeholders alternating placeholder names and values
   * @return rendered component
   */
  public synchronized Component component(String key, Object... placeholders) {
    String template = templates.get(key);
    if (template == null) {
      if (reportedMissingKeys.add(key)) {
        plugin.getLogger().warning("Missing locale key: " + key);
      }
      template = "<red>[missing message: " + key + "]</red>";
    }
    return miniMessage.deserialize(prepareForDeserialize(template), resolvers(placeholders));
  }

  public String text(String key, Object... placeholders) {
    return LegacyComponentSerializer.legacySection().serialize(component(key, placeholders));
  }

  public String plain(String key, Object... placeholders) {
    return PlainTextComponentSerializer.plainText().serialize(component(key, placeholders));
  }

  private TagResolver resolvers(Object... placeholders) {
    TagResolver.Builder builder =
        TagResolver.builder().resolver(Placeholder.parsed("prefix", prefixTemplate));
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
