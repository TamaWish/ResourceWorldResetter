package io.github.tamawish.rwr.teleport;

public record TeleportAttempt(boolean successful, String message) {
    public static TeleportAttempt success(String message) {
        return new TeleportAttempt(true, message);
    }

    public static TeleportAttempt failure(String message) {
        return new TeleportAttempt(false, message);
    }
}
