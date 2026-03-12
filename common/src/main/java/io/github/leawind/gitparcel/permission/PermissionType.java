package io.github.leawind.gitparcel.permission;

import io.github.leawind.gitparcel.GitParcelTranslations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * @see PermissionLevel
 */
public record PermissionType<T>(byte id, String name, PermissionLevel defaultLevel) {

  public Component translation() {
    return GitParcelTranslations.of("gitparcel.permission." + name);
  }
}
