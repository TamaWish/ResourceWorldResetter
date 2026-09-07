package io.github.tamawish.rwr.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class WorldIdentityTest {
  private final WorldCatalogView catalog =
      new WorldCatalogView() {
        @Override
        public Set<String> registeredWorldNames() {
          return Set.of("world", "minecraft:overworld", "hub", "worlds:hub");
        }

        @Override
        public String defaultWorldName() {
          return "world";
        }

        @Override
        public String canonicalWorldName(String name) {
          return switch (name) {
            case "world", "minecraft:overworld" -> "minecraft:overworld";
            case "hub", "worlds:hub" -> "worlds:hub";
            default -> name;
          };
        }
      };

  @Test
  void aliasesCannotBypassDefaultOrHubProtection() {
    assertThat(WorldStateResolver.resolve("minecraft:overworld", true, true, "hub", catalog))
        .isEqualTo(WorldOperationalState.PROTECTED);
    assertThat(WorldStateResolver.resolve("worlds:hub", true, true, "hub", catalog))
        .isEqualTo(WorldOperationalState.PROTECTED);
  }
}
