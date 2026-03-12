package io.github.leawind.gitparcel.utils.permission;

import net.minecraft.server.permissions.PermissionLevel;

/**
 * @see PermissionLevel
 */
public record PermissionType<T>(byte id, String name, PermissionLevel defaultLevel) {}
