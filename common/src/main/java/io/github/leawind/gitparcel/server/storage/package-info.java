/**
 * Storage management for GitParcel mod.
 *
 * <p>This package provides a layered storage architecture:
 *
 * <ul>
 *   <li>{@link io.github.leawind.gitparcel.server.storage.StorageManager} - Central coordinator for
 *       all storage operations
 *   <li>{@link io.github.leawind.gitparcel.server.storage.GameStorageManager} - Game
 *       instance-specific storage
 *   <li>{@link io.github.leawind.gitparcel.server.storage.SystemStorageManager} - System-wide
 *       storage following XDG specification
 *   <li>{@link io.github.leawind.gitparcel.server.storage.SecretManager} - Secure storage for
 *       sensitive credentials
 * </ul>
 *
 * <p>Storage locations can be customized via environment variables:
 *
 * <ul>
 *   <li>{@code GITPARCEL_SHARE_DIR} - Override shared content directory
 *   <li>{@code GITPARCEL_CACHE_DIR} - Override cache directory
 * </ul>
 */
package io.github.leawind.gitparcel.server.storage;
