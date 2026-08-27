package com.lozaine.resourceworldresetter.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PluginVersionTest {
    @Test
    void parsesReleaseAndQualifiedVersions() {
        assertThat(PluginVersion.parse("5.8.0")).isEqualTo(new PluginVersion(5, 8, 0));
        assertThat(PluginVersion.parse("5.8.1-SNAPSHOT")).isEqualTo(new PluginVersion(5, 8, 1));
        assertThat(PluginVersion.parse("5.8")).isEqualTo(new PluginVersion(5, 8, 0));
    }

    @Test
    void comparesNumerically() {
        assertThat(new PluginVersion(5, 8, 0)).isGreaterThan(new PluginVersion(5, 7, 9));
        assertThat(new PluginVersion(6, 0, 0)).isGreaterThan(new PluginVersion(5, 99, 99));
    }

    @Test
    void rejectsUnknownText() {
        assertThatIllegalArgumentException().isThrownBy(() -> PluginVersion.parse("dev-build"));
    }
}
