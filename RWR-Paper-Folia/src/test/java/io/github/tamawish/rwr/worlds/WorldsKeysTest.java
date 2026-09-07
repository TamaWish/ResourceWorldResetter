package io.github.tamawish.rwr.worlds;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class WorldsKeysTest {
  @Test
  void customNamespaceDoesNotMatchAnUnrelatedBareWorldName() {
    assertThat(WorldsKeys.keyMatchesName(Key.key("worlds", "resource"), "resource")).isFalse();
    assertThat(WorldsKeys.keyMatchesName(Key.key("worlds", "resource"), "worlds_resource"))
        .isTrue();
    assertThat(WorldsKeys.keyMatchesName(Key.key("worlds", "resource"), "worlds:resource"))
        .isTrue();
    assertThat(WorldsKeys.keyMatchesName(Key.key("minecraft", "resource"), "resource")).isTrue();
  }
}
