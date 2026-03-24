package io.github.leawind.gitparcel.utils.permission;

import net.minecraft.server.permissions.PermissionLevel;

/**
 * An immutable definition of a single permission type.
 *
 * <p>Permission types are defined as static constants in domain-specific permission classes (e.g.
 * {@code ParcelPermissions.SAVE}, {@code WorldPermissions.CREATE_PARCEL}) and registered into a
 * {@link PermissionTypeRegistry}.
 *
 * @param id unique string identifier within its registry
 * @param defaultLevel the {@link PermissionLevel} used when no override is set in a {@link
 *     PermissionConfig}
 * @param <T> type-safety tag to prevent mixing types from different registries
 */
public record PermissionType<T>(String id, PermissionLevel defaultLevel) {}
