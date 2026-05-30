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
 * </ul>
 */
package io.github.leawind.gitparcel.server.storage;
