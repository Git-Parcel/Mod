package io.github.leawind.gitparcel.server.minecraft.logic.storage;

import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;

/**
 * Central coordinator for all GitParcel storage operations.
 *
 * <p>Manages the lifecycle and access to game-specific and system-wide storage, providing unified
 * access to shared content and cached data.
 */
public final class StorageUtils {
  private StorageUtils() {}

  // Environment variable names
  private static final String ENV_SHARE_DIR = "GITPARCEL_SHARE_DIR";
  private static final String ENV_CACHE_DIR = "GITPARCEL_CACHE_DIR";

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

  /**
   * Resolves the actual shared content directory.
   *
   * <p>Priority:
   *
   * <ol>
   *   <li>Environment variable {@code GITPARCEL_SHARE_DIR}
   *   <li>{@code GameStorageManager.Config.useSystemStorage} — if true, uses system shared dir;
   *       otherwise uses game shared dir
   * </ol>
   *
   * @param server the Minecraft server instance
   * @return the resolved shared directory path
   * @throws IOException if the game config cannot be read
   */
  public static Path getSharedDir(MinecraftServer server) throws IOException {
    var env = System.getenv(ENV_SHARE_DIR);
    if (env != null && !env.isBlank()) {
      return Path.of(env).normalize();
    }

    var gameConfig = gameStorage(server).getConfig();
    if (gameConfig.useSystemStorage()) {
      return Path.of(SystemStorageManager.getSharedDir().toString()).normalize();
    }
    return gameStorage(server).getSharedDir().normalize();
  }

  /**
   * Resolves the actual cache directory.
   *
   * <p>Priority:
   *
   * <ol>
   *   <li>Environment variable {@code GITPARCEL_CACHE_DIR}
   *   <li>Default: {@code GameStorageManager.getCachedDir()}
   * </ol>
   *
   * @param server the Minecraft server instance
   * @return the resolved cache directory path
   */
  public static Path getCacheDir(MinecraftServer server) {
    var env = System.getenv(ENV_CACHE_DIR);
    if (env != null && !env.isBlank()) {
      return Path.of(env).normalize();
    }
    return gameStorage(server).getCachedDir().normalize();
  }
}
