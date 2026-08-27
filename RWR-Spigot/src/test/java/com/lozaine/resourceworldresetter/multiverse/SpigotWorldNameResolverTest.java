package com.lozaine.resourceworldresetter.multiverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SpigotWorldNameResolverTest {
    @Test
    void loadedBukkitNameWinsOverNamespacedMultiverseName() {
        assertThat(MultiverseApiGateway.resolveSpigotSafeWorldName(
                        "Rainforest", "minecraft:rainforest", "rainforest", "minecraft:rainforest"))
                .isEqualTo("Rainforest");
    }

    @Test
    void usesPlainMultiverseLegacyNameWhenBukkitNameIsUnavailable() {
        assertThat(MultiverseApiGateway.resolveSpigotSafeWorldName(
                        null, "Rainforest", "rainforest", "minecraft:rainforest"))
                .isEqualTo("Rainforest");
    }

    @Test
    void usesNamespacedKeyPathWhenOtherNamesAreUnavailable() {
        assertThat(MultiverseApiGateway.resolveSpigotSafeWorldName(
                        null, "minecraft:rainforest", "rainforest", "minecraft:rainforest"))
                .isEqualTo("rainforest");
    }

    @Test
    void stripsTheLastNamespaceAsFinalFallback() {
        assertThat(MultiverseApiGateway.resolveSpigotSafeWorldName(
                        null, "bad namespace:Rainforest", null, "minecraft:rainforest"))
                .isEqualTo("Rainforest");
    }

    @Test
    void rejectsInvalidNameBeforeDeletionCanStart() {
        assertThatThrownBy(() -> MultiverseApiGateway.resolveSpigotSafeWorldName(
                        null, "bad world name", null, "bad identity"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Spigot-safe world name");
    }

    @Test
    void preservesLongPlainMultiverseNameExactly() {
        String longName = "rainforest_resource_world_season_twenty_six";

        assertThat(MultiverseApiGateway.resolveSpigotSafeWorldName(
                        null, longName, longName, "minecraft:" + longName))
                .isEqualTo(longName);
    }
}
