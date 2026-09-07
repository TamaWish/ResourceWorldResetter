package io.github.tamawish.rwr.worlds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorldsLifecycleListenerTest {
  @Test
  void deletionMatchesWorldIdentityWithoutDroppingAnotherNamespace() {
    assertThat(WorldsLifecycleListener.matches("worlds:resource", "resource", "worlds:resource"))
        .isTrue();
    assertThat(WorldsLifecycleListener.matches("resource", "resource", "worlds:resource")).isTrue();
    assertThat(WorldsLifecycleListener.matches("other:resource", "resource", "worlds:resource"))
        .isFalse();
    assertThat(WorldsLifecycleListener.matches("different", "resource", "worlds:resource"))
        .isFalse();
  }
}
