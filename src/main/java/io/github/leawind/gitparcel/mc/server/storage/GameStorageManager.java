package io.github.leawind.gitparcel.mc.server.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.core.GitParcel;
import io.github.leawind.gitparcel.mc.server.storage.cached.CachedContent;
import io.github.leawind.gitparcel.mc.server.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
  public static final String DIR_NAME = GitParcel.MOD_ID;

  /** Configuration file name. */
  public static final String CONFIG_FILE_NAME = "config.json";

  /** Gson instance configured for pretty-printed JSON output. */
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final ConcurrentHashMap<Path, GameStorageManager> CACHE =
      new ConcurrentHashMap<>();

  public static GameStorageManager getInstance(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server.getServerDirectory().resolve(DIR_NAME).normalize(), GameStorageManager::new);
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
   * Saves the current game configuration to disk.
   *
   * @throws IOException if an I/O error occurs
   * @throws IllegalStateException if the config has not been loaded yet
   */
  public void saveConfig() throws IOException {
    if (config == null) {
      throw new IllegalStateException("Config has not been loaded yet");
    }
    config.save(configFile);
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
                            .optionalFieldOf("useSystemStorage", false)
                            .forGetter(Config::useSystemStorage))
                    .apply(inst, Config::new));

    /**
     * Loads the config from the specified file.
     *
     * @param file the config file path
     * @return the loaded config
     * @throws IOException if the file not exist or cannot be read
     */
    private static Config load(Path file) throws IOException {
      var json = GSON.fromJson(Files.readString(file), JsonObject.class);
      return Config.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private void save(Path file) throws IOException {
      Files.createDirectories(file.getParent());
      var result = CODEC.encodeStart(JsonOps.INSTANCE, this);
      Files.writeString(file, GSON.toJson((JsonObject) result.getOrThrow()));
    }

    private boolean useSystemStorage = false;

    private Config() {}

    private Config(boolean useSystemStorage) {
      this.useSystemStorage = useSystemStorage;
    }

    /**
     * Checks whether system storage is enabled.
     *
     * @return true if system storage should be used
     */
    public boolean useSystemStorage() {
      return useSystemStorage;
    }

    /**
     * Sets whether to use system storage.
     *
     * @param value true to enable system storage
     */
    public void useSystemStorage(boolean value) {
      this.useSystemStorage = value;
    }
  }
}
