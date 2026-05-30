package io.github.leawind.gitparcel.server.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.server.storage.cached.CachedContent;
import io.github.leawind.gitparcel.server.storage.shared.SharedContent;
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
 */
public final class StorageManager {
  /** Logger instance for storage operations. */
  public static final Logger LOGGER = LogUtils.getLogger();

  /** Gson instance configured for pretty-printed JSON output. */
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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

  public Path resolveSharedDir() {
    return SystemStorageManager.getDataDir();
  }

  public Path resolveCachedDir() {
    return SystemStorageManager.getCacheDir();
  }
}
