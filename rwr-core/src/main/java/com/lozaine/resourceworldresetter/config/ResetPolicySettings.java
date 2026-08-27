package com.lozaine.resourceworldresetter.config;

public record ResetPolicySettings(int maxSafeRetries, int retryDelaySeconds, boolean broadcastCompletion) {}
