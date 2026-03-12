package io.github.leawind.gitparcel.permission;

public interface ReadablePermissionSettings {
  PermissionTypeRegistry getRegistry();

  byte get(PermissionType type);
}
