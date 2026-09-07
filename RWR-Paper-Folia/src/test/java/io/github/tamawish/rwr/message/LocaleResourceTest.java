package io.github.tamawish.rwr.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LocaleResourceTest {
  private static final String[] LOCALES = {"ja_JP", "ko_KR", "zh_CN"};

  @Test
  void translationsContainExactlyTheEnglishMessageKeys() {
    Set<String> english = scalarKeys(load("en_US"));

    for (String locale : LOCALES) {
      assertThat(scalarKeys(load(locale)))
          .as("%s locale keys", locale)
          .containsExactlyInAnyOrderElementsOf(english);
    }
  }

  private static YamlConfiguration load(String locale) {
    String path = "locales/" + locale + ".yml";
    InputStream stream = LocaleResourceTest.class.getClassLoader().getResourceAsStream(path);
    assertThat(stream).as(path).isNotNull();
    return YamlConfiguration.loadConfiguration(
        new InputStreamReader(stream, StandardCharsets.UTF_8));
  }

  private static Set<String> scalarKeys(YamlConfiguration locale) {
    Set<String> keys = new TreeSet<>();
    for (String key : locale.getKeys(true)) {
      if (!locale.isConfigurationSection(key)) {
        keys.add(key);
      }
    }
    return keys;
  }
}
