package io.github.tamawish.rwr.config;

/**
 * Global retry and notification policy for reset operations.
 *
 * @param maxSafeRetries maximum automatic retries for safely retryable failures
 * @param retryDelaySeconds delay between automatic retry attempts
 * @param broadcastCompletion whether successful completion is broadcast
 */
public record ResetPolicySettings(
    int maxSafeRetries, int retryDelaySeconds, boolean broadcastCompletion) {}
