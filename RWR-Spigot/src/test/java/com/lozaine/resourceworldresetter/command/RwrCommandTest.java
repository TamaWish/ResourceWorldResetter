package com.lozaine.resourceworldresetter.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RwrCommandTest {
    @Test
    void completionIsCaseInsensitiveSortedAndPrefixBounded() {
        assertThat(RwrCommand.matching("R", List.of("status", "reload", "reset", "gui")))
                .containsExactly("reload", "reset");
    }
}
