package com.lozaine.resourceworldresetter.multiverse;

import org.mvplugins.multiverse.core.locale.message.Message;

/** Converts Multiverse message values to useful log text instead of object identities. */
public final class MultiverseFailureMessages {
    private MultiverseFailureMessages() {}

    public static String format(Message message) {
        if (message == null) {
            return "Multiverse returned no failure message.";
        }
        try {
            String formatted = message.formatted();
            if (formatted != null && !formatted.isBlank()) {
                return formatted;
            }
        } catch (RuntimeException ignored) {
            // A localized manager may not be available during shutdown; raw text is still useful.
        }
        String raw = message.rawFormatted();
        return raw == null || raw.isBlank() ? "Multiverse returned an empty failure message." : raw;
    }
}
