package io.github.leawind.gitparcel.storage;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.storage.cached.CachedContent;
import io.github.leawind.gitparcel.storage.shared.SharedContent;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Manages game instance storage.
 *
 * <p>This includes:
 *
 * <ul>
 *   <li>Config - Game-specific configuration
 *   <li>Shared - Shareable content (when system storage is disabled)
 *   <li>Cached - Download cache for packaged parcels
 * </ul>
 */
public class GameStorageManager {
  /** Subdirectory name within the game server directory. */
  public static final String DIR_NAME = GitParcelMod.MOD_ID;

  /** Configuration file name. */
  public static final String CONFIG_FILE_NAME = "config.json";

  private static final Map<Path, WeakReference<GameStorageManager>> CACHE =
      new ConcurrentHashMap<>();

  public static GameStorageManager getInstance(MinecraftServer server) {
    var directory = server.getServerDirectory().resolve(DIR_NAME).normalize();
    WeakReference<GameStorageManager> ref =
        CACHE.compute(
            directory,
            (key, currentRef) -> {
              if (currentRef != null && currentRef.get() != null) {
                return currentRef;
              }
              return new WeakReference<>(new GameStorageManager(directory));
            });

    return ref.get();
  }

  private final Path root;
  private final Path configFile;
  private final Path sharedDir;
  private final Path cachedDir;

  private @Nullable Config config;

  /**
   * Creates a GameStorageManager for the specified root directory.
   *
   * @param root the game storage root directory
   */
  GameStorageManager(Path root) {
    this.root = root;
    this.configFile = root.resolve(CONFIG_FILE_NAME);
    this.sharedDir = root.resolve(SharedContent.SHARED_DIR_NAME);
    this.cachedDir = root.resolve(CachedContent.CACHED_DIR_NAME);
  }

  /**
   * Gets the game storage root directory.
   *
   * @return the game storage directory path
   */
  public Path getRoot() {
    return root;
  }

  public Path getConfigFile() {
    return configFile;
  }

  /**
   * Gets the game configuration, loading it from disk if necessary.
   *
   * @return the game configuration
   * @throws IOException if the configuration file exist but cannot be loaded
   */
  public Config getConfig() throws IOException {
    if (config == null) {
      if (Files.exists(configFile)) {
        config = Config.load(configFile);
      } else {
        config = new Config();
      }
    }
    return config;
  }

  /**
   * Gets the path to the shared content directory.
   *
   * @return the shared content directory path
   */
  public Path getSharedDir() {
    return sharedDir;
  }

  /**
   * Gets the path to the cached content directory.
   *
   * @return the cached content directory path
   */
  public Path getCachedDir() {
    return cachedDir;
  }

  /**
   * Game-specific configuration.
   *
   * <p>Stored in the game storage directory.
   */
  public static final class Config {
    private static final Codec<Config> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.BOOL
                            .optionalFieldOf("isUseSystemStorage", false)
                            .forGetter(Config::isUseSystemStorage))
                    .apply(inst, Config::new));

    /**
     * Loads the config from the specified file.
     *
     * @param file the config file path
     * @return the loaded config
     * @throws IOException if the file not exist or cannot be read
     */
    private static Config load(Path file) throws IOException {
      var json = StorageManager.GSON.fromJson(Files.readString(file), JsonObject.class);
      return Config.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private void save(Path file) throws IOException {
      Files.createDirectories(file.getParent());
      var result = CODEC.encodeStart(JsonOps.INSTANCE, this);
      Files.writeString(file, StorageManager.GSON.toJson((JsonObject) result.getOrThrow()));
    }

    private boolean isUseSystemStorage = false;

    private Config() {}

    private Config(boolean isUseSystemStorage) {
      this.isUseSystemStorage = isUseSystemStorage;
    }

    /**
     * Checks whether system storage is enabled.
     *
     * @return true if system storage should be used
     */
    public boolean isUseSystemStorage() {
      return isUseSystemStorage;
    }

    /**
     * Sets whether to use system storage.
     *
     * @param value true to enable system storage
     */
    public void setUseSystemStorage(boolean value) {
      this.isUseSystemStorage = value;
    }
  }
}
