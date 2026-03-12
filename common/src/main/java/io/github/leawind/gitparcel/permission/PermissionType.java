package io.github.leawind.gitparcel.permission;

import io.github.leawind.gitparcel.GitParcelTranslations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * @see PermissionLevel
 */
public record PermissionType(byte id, String name, byte defaultLevel) {

  public PermissionType {

    // Check id
    if (id < 0 || id > 63) {
      throw new IllegalArgumentException("id must be in range [0, 63], got: " + id);
    }
  }

  public long mask() {
    return 1L << id;
  }

  public Component translation() {
    return GitParcelTranslations.of("gitparcel.permission." + name);
  }
}
