package io.github.leawind.gitparcel.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.storage.cached.CachedContent;
import io.github.leawind.gitparcel.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Central coordinator for all GitParcel storage operations.
 *
 * <p>Manages the lifecycle and access to game-specific and system-wide storage, providing unified
 * access to shared content and cached data.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 * // Create from Minecraft server
 * StorageManager storage = StorageManager.fromServer(server);
 *
 * // Create from game instance path
 * StorageManager storage = StorageManager.fromPath(gamePath);
 *
 * // Access storage managers
 * GameStorageManager game = storage.getGameStorage();
 * SystemStorageManager system = storage.getSystemStorage();
 *
 * // Get content managers
 * SharedContent shared = storage.getSharedContent();
 * CachedContent cached = storage.getCachedContent();
 * }</pre>
 */
public final class StorageManager {
  /** Logger instance for storage operations. */
  public static final Logger LOGGER = LogUtils.getLogger();

  /** Gson instance configured for pretty-printed JSON output. */
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  /** Environment variable to override shared content directory. */
  public static final String ENV_SHARE_DIR = "GITPARCEL_SHARE_DIR";

  /** Environment variable to override cache directory. */
  public static final String ENV_CACHE_DIR = "GITPARCEL_CACHE_DIR";

  public static Map<MinecraftServer, StorageManager> CACHE = new ConcurrentHashMap<>();

  public static StorageManager getInstance(MinecraftServer server) {
    return CACHE.computeIfAbsent(server, StorageManager::new);
  }

  private final MinecraftServer server;

  private @Nullable SharedContent sharedContent;
  private @Nullable CachedContent cachedContent;

  private StorageManager(MinecraftServer server) {
    this.server = server;
  }

  /**
   * Gets the system-wide storage manager.
   *
   * @return the system storage manager
   */
  public SystemStorageManager systemStorage() {
    return SystemStorageManager.getInstance();
  }

  /**
   * Gets the game instance storage manager.
   *
   * @return the game storage manager
   */
  public GameStorageManager gameStorage() {
    return GameStorageManager.getInstance(server);
  }

  /**
   * Gets the world storage manager.
   *
   * @return the world storage manager
   */
  public WorldStorageManager worldStorage() {
    return WorldStorageManager.getInstance(server);
  }

  /**
   * Gets the shared content manager, creating it if necessary.
   *
   * @return the shared content manager
   * @throws IOException if the shared directory cannot be accessed
   */
  public SharedContent getSharedContent() throws IOException {
    var sharedDir = resolveSharedDir();
    if (sharedContent == null || sharedContent.getRoot() != sharedDir) {
      sharedContent = new SharedContent(sharedDir);
    }
    return sharedContent;
  }

  /**
   * Gets the cached content manager, creating it if necessary.
   *
   * @return the cached content manager
   * @throws IOException if the cache directory cannot be accessed
   */
  public CachedContent getCachedContent() throws IOException {
    var cacheDir = resolveCachedDir();
    if (cachedContent == null || cachedContent.getRoot() != cacheDir) {
      cachedContent = new CachedContent(cacheDir);
    }
    return cachedContent;
  }

  /**
   * Resolves the path to the shared content directory.
   *
   * <p>Resolution order:
   *
   * <ol>
   *   <li>{@link #ENV_SHARE_DIR} environment variable
   *   <li>System storage if enabled in game config
   *   <li>Game storage directory
   * </ol>
   *
   * @return the shared content directory path
   * @throws IOException if the path cannot be resolved
   */
  public Path resolveSharedDir() throws IOException {
    return resolveStoragePath(
        ENV_SHARE_DIR, systemStorage()::getSharedDir, gameStorage()::getSharedDir);
  }

  /**
   * Resolves the path to the cached content directory.
   *
   * <p>Resolution order:
   *
   * <ol>
   *   <li>{@link #ENV_CACHE_DIR} environment variable
   *   <li>System storage if enabled in game config
   *   <li>Game storage directory
   * </ol>
   *
   * @return the cached content directory path
   * @throws IOException if the path cannot be resolved
   */
  public Path resolveCachedDir() throws IOException {
    return resolveStoragePath(
        ENV_CACHE_DIR, systemStorage()::getCachedDir, gameStorage()::getCachedDir);
  }

  /**
   * Resolves a storage path using the standard resolution order.
   *
   * @param envVar the environment variable to check first
   * @param systemPathSupplier supplier for system storage path
   * @param gamePathSupplier supplier for game storage path
   * @return the resolved path
   * @throws IOException if the path cannot be resolved
   */
  private Path resolveStoragePath(
      String envVar, PathSupplier systemPathSupplier, PathSupplier gamePathSupplier)
      throws IOException {
    // 1. Check environment variable
    var envPath = System.getenv(envVar);
    if (envPath != null) {
      return Path.of(envPath);
    }

    // 2. Check if system storage is enabled
    if (gameStorage().getConfig().useSystemStorage()) {
      return systemPathSupplier.get();
    }

    // 3. Fall back to game storage
    return gamePathSupplier.get();
  }

  @FunctionalInterface
  private interface PathSupplier {
    Path get() throws IOException;
  }
}
