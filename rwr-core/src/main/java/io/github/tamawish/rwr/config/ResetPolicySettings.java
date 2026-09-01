package io.github.tamawish.rwr.config;

public record ResetPolicySettings(int maxSafeRetries, int retryDelaySeconds, boolean broadcastCompletion) {}
