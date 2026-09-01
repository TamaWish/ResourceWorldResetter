package io.github.tamawish.rwr.message;

import java.util.Map;

/**
 * Platform-neutral message delivery. Implementations render MiniMessage / Adventure.
 */
public interface MessageSink {
    void send(Object audience, String key, Map<String, String> placeholders);

    void broadcast(String key, Map<String, String> placeholders);

    String plain(String key, Map<String, String> placeholders);
}
