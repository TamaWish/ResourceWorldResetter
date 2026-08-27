package com.lozaine.resourceworldresetter.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageServiceTest {
    @Test
    void convertsLegacyPercentPlaceholdersToMiniMessageTags() {
        assertThat(MessageService.percentToMiniMessage("%prefix%%world% resets at %time% (%unknown%)"))
                .isEqualTo("<prefix><world> resets at <time> (<unknown>)");
    }

    @Test
    void convertsAmpersandColorCodesToMiniMessageTags() {
        assertThat(MessageService.ampersandToMiniMessage("&6[RWR]&r &cFailed"))
                .isEqualTo("<gold>[RWR]<reset> <red>Failed");
    }

    @Test
    void normalizeAppliesPercentThenAmpersandConversion() {
        assertThat(MessageService.normalizeTemplate("%prefix%&eHello &f%world%"))
                .isEqualTo("<prefix><yellow>Hello <white><world>");
    }
}
