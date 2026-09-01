package io.github.tamawish.rwr.multiverse;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class RegenerationRequestTest {
    @Test
    void fixedPolicyRequiresSeed() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegenerationRequest("resource", SeedPolicy.FIXED, null, true, true, true));
    }

    @Test
    void nonFixedPolicyRejectsSeed() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegenerationRequest("resource", SeedPolicy.SAME, 42L, true, true, true));
    }

    @Test
    void blankWorldIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RegenerationRequest(" ", SeedPolicy.RANDOM, null, true, true, true));
    }
}
