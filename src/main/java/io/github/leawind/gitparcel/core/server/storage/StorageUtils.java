package io.github.leawind.gitparcel.core.server.storage;

import net.minecraft.server.MinecraftServer;

/**
 * Central coordinator for all GitParcel storage operations.
 *
 * <p>Manages the lifecycle and access to game-specific and system-wide storage, providing unified
 * access to shared content and cached data.
 */
public final class StorageUtils {
  private StorageUtils() {}

  /**
   * Gets the game instance storage manager.
   *
   * @return the game storage manager
   */
  public static GameStorageManager gameStorage(MinecraftServer server) {
    return GameStorageManager.getInstance(server);
  }

  /**
   * Gets the world storage manager.
   *
   * @return the world storage manager
   */
  public static WorldStorageManager worldStorage(MinecraftServer server) {
    return WorldStorageManager.getInstance(server);
  }
}
