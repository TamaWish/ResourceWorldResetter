package io.github.tamawish.rwr.multiverse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
class RegenerationOptionsMapperTest {
    @Test
    void plansSeedAndPreservationFlags() {
        RegenerationRequest request = new RegenerationRequest(
                "resource", SeedPolicy.RANDOM, null, true, false, true);

        RegenerationOptionsPlan plan = RegenerationOptionsPlan.from(request);

        assertThat(plan.seedPolicy()).isEqualTo(SeedPolicy.RANDOM);
        assertThat(plan.fixedSeed()).isNull();
        assertThat(plan.keepWorldConfig()).isTrue();
        assertThat(plan.keepGameRules()).isFalse();
        assertThat(plan.keepWorldBorder()).isTrue();
    }

    @Test
    void retainsFixedSeedExactly() {
        RegenerationOptionsPlan fixed = RegenerationOptionsPlan.from(
                new RegenerationRequest("resource", SeedPolicy.FIXED, -922L, true, true, true));

        assertThat(fixed.seedPolicy()).isEqualTo(SeedPolicy.FIXED);
        assertThat(fixed.fixedSeed()).isEqualTo(-922L);
    }
}
