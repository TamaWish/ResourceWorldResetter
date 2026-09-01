package io.github.tamawish.rwr.config;

public record ConfigIssue(String path, String message) {
    @Override
    public String toString() {
        return path + ": " + message;
    }
}
