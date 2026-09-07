package io.github.tamawish.rwr.teleport;

/** Tests permissions without exposing a platform sender type. */
@FunctionalInterface
public interface PermissionChecker {
  boolean hasPermission(String permission);
}
