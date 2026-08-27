package com.lozaine.resourceworldresetter.teleport;

@FunctionalInterface
public interface PermissionChecker {
    boolean hasPermission(String permission);
}
