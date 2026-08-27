package com.lozaine.resourceworldresetter.multiverse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mvplugins.multiverse.core.locale.message.Message;

class MultiverseFailureMessagesTest {
    @Test
    void formatsMessageContentInsteadOfJavaObjectIdentity() {
        String formatted = MultiverseFailureMessages.format(Message.of("World creation failed for Reso"));

        assertThat(formatted).isEqualTo("World creation failed for Reso");
        assertThat(formatted).doesNotContain("LocalizedMessage@");
    }

    @Test
    void handlesMissingFailureMessage() {
        assertThat(MultiverseFailureMessages.format(null)).contains("no failure message");
    }
}
