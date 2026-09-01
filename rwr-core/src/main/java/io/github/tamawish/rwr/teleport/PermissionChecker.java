package io.github.tamawish.rwr.teleport;

@FunctionalInterface
public interface PermissionChecker {
    boolean hasPermission(String permission);
}
