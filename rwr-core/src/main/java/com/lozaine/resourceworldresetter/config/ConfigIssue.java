package com.lozaine.resourceworldresetter.config;

public record ConfigIssue(String path, String message) {
    @Override
    public String toString() {
        return path + ": " + message;
    }
}
