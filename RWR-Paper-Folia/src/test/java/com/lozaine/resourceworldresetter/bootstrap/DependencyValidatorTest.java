package com.lozaine.resourceworldresetter.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DependencyValidatorTest {
    @Test
    void acceptsMinimumWorldsVersion() {
        DependencyValidator.ValidationResult result = DependencyValidator.validateVersion("4.4.0");

        assertThat(result.compatible()).isTrue();
        assertThat(result.installedVersion()).isEqualTo(new PluginVersion(4, 4, 0));
    }

    @Test
    void rejectsOlderWorldsVersion() {
        DependencyValidator.ValidationResult result = DependencyValidator.validateVersion("4.3.9");

        assertThat(result.compatible()).isFalse();
        assertThat(result.message()).contains("4.4.0 or newer", "4.3.9");
    }

    @Test
    void rejectsUnparseableWorldsVersion() {
        DependencyValidator.ValidationResult result = DependencyValidator.validateVersion("development");

        assertThat(result.compatible()).isFalse();
        assertThat(result.message()).contains("Cannot parse", "development");
    }
}
