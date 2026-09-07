package io.github.tamawish.rwr.teleport;

/**
 * Reports whether a teleport request was accepted.
 *
 * @param successful whether teleportation completed
 * @param message user-facing outcome message
 */
public record TeleportAttempt(boolean successful, String message) {
  public static TeleportAttempt success(String message) {
    return new TeleportAttempt(true, message);
  }

  public static TeleportAttempt failure(String message) {
    return new TeleportAttempt(false, message);
  }
}
