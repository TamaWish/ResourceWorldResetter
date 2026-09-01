package io.github.tamawish.rwr.worlds;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tamawish.rwr.multiverse.RegenerationRequest;
import io.github.tamawish.rwr.multiverse.SeedPolicy;
import org.junit.jupiter.api.Test;

class WorldsRegenerationPlanTest {
    @Test
    void sameSeedLeavesTheWorldsBuilderSeedUnchanged() {
        WorldsRegenerationPlan plan = WorldsRegenerationPlan.from(
                new RegenerationRequest("worlds:resource", SeedPolicy.SAME, null, true, true, true),
                42L);

        assertThat(plan.seed()).isNull();
        assertThat(plan.ignoreLevelData()).isFalse();
    }

    @Test
    void fixedSeedIsForwardedAndDiscardingWorldConfigIgnoresLevelData() {
        WorldsRegenerationPlan plan = WorldsRegenerationPlan.from(
                new RegenerationRequest("worlds:resource", SeedPolicy.FIXED, 12345L, false, true, true),
                42L);

        assertThat(plan.seed()).isEqualTo(12345L);
        assertThat(plan.ignoreLevelData()).isTrue();
    }

    @Test
    void randomSeedDiffersFromCurrentSeed() {
        WorldsRegenerationPlan plan = WorldsRegenerationPlan.from(
                new RegenerationRequest("worlds:resource", SeedPolicy.RANDOM, null, true, true, true),
                42L);

        assertThat(plan.seed()).isNotNull().isNotEqualTo(42L);
    }
}
