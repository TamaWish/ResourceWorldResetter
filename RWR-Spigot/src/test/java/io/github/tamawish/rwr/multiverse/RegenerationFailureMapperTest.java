package io.github.tamawish.rwr.multiverse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegenerationFailureMapperTest {
    @Test
    void mapsEveryMultiverseRegenerationFailure() {
        assertThat(RegenerationFailureMapper.map(
                        org.mvplugins.multiverse.core.world.reasons.RegenFailureReason.DELETE_FAILED))
                .isEqualTo(RegenerationFailureReason.DELETE_FAILED);
        assertThat(RegenerationFailureMapper.map(
                        org.mvplugins.multiverse.core.world.reasons.RegenFailureReason.CREATE_FAILED))
                .isEqualTo(RegenerationFailureReason.CREATE_FAILED);
    }
}
