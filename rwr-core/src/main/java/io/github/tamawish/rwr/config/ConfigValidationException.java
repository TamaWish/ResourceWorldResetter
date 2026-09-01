package io.github.tamawish.rwr.config;

import java.util.List;

public final class ConfigValidationException extends Exception {
    private final List<ConfigIssue> issues;

    public ConfigValidationException(List<ConfigIssue> issues) {
        super("Configuration is invalid: " + issues);
        this.issues = List.copyOf(issues);
    }

    public List<ConfigIssue> issues() {
        return issues;
    }
}
