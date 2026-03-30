package io.github.leawind.gitparcel.storage;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dirs.BaseDirectories;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.storage.cached.CachedContent;
import io.github.leawind.gitparcel.storage.shared.SharedContent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Manages system-level storage.
 *
 * <p>Follows XDG specification using the {@code dev.dirs:directories} library for cross-platform
 * directory management.
 */
public class SystemStorageManager {
  /** Namespace used for directory paths. */
  public static final String DIR_NAME = GitParcelMod.MOD_ID;

  /** Configuration file name. */
  public static final String CONFIG_FILE_NAME = "config.json";

  private static final String SECRET_DIR_NAME = "secrets";

  private static @Nullable SystemStorageManager instance = null;

  public static SystemStorageManager getInstance() {
    if (instance == null) {
      instance = create(BaseDirectories.get());
    }
    return instance;
  }

  public static SystemStorageManager create(BaseDirectories directories) {
    var configFile = Path.of(directories.configDir).resolve(DIR_NAME).resolve(CONFIG_FILE_NAME);
    var secretDir = Path.of(directories.dataLocalDir).resolve(DIR_NAME).resolve(SECRET_DIR_NAME);

    var dataDir = Path.of(directories.dataDir);
    var defaultSharedDir = dataDir.resolve(DIR_NAME).resolve(SharedContent.SHARED_DIR_NAME);
    var defaultCachedDir = dataDir.resolve(DIR_NAME).resolve(CachedContent.CACHED_DIR_NAME);

    return new SystemStorageManager(configFile, secretDir, defaultSharedDir, defaultCachedDir);
  }

  private final Path configFile;
  private final Path secretDir;

  /** The default shared content directory, can be overridden by {@link Config#sharedDir}. */
  private final Path defaultSharedDir;

  /** The default cached content directory, can be overridden by {@link Config#cachedDir}. */
  private final Path defaultCachedDir;

  private @Nullable Config config;
  private @Nullable SecretManager secretManager;

  private SystemStorageManager(
      Path configFile, Path secretDir, Path defaultSharedDir, Path defaultCachedDir) {
    this.configFile = configFile;
    this.secretDir = secretDir;
    this.defaultSharedDir = defaultSharedDir;
    this.defaultCachedDir = defaultCachedDir;
  }

  /**
   * Gets the system configuration, loading it from disk if necessary.
   *
   * @return the system configuration
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
   * Gets the secret manager for managing sensitive data.
   *
   * @return the secret manager
   */
  public SecretManager getSecretManager() {
    if (secretManager == null) {
      secretManager = new SecretManager(secretDir);
    }
    return secretManager;
  }

  /**
   * Gets the configuration file path.
   *
   * @return the configuration file path
   */
  public Path getConfigFile() {
    return configFile;
  }

  /**
   * Gets the secret directory path.
   *
   * @return the secret directory path
   */
  public Path getSecretDir() {
    return secretDir;
  }

  /**
   * Gets the default shared content directory.
   *
   * @return the default shared content directory path
   */
  public Path getDefaultSharedDir() {
    return defaultSharedDir;
  }

  /**
   * Gets the effective shared content directory, considering custom path configuration.
   *
   * @return the effective shared content directory path
   * @throws IOException if the system configuration cannot be loaded
   * @throws InvalidPathException if the custom path is invalid
   */
  public Path getSharedDir() throws IOException, InvalidPathException {
    var customPath = getConfig().getSharedDir();
    return customPath != null ? Path.of(customPath) : defaultSharedDir;
  }

  /**
   * Gets the effective cached content directory, considering custom path configuration.
   *
   * @return the effective cached content directory path
   * @throws IOException if the system configuration cannot be loaded
   * @throws InvalidPathException if the custom path is invalid
   */
  public Path getCachedDir() throws IOException, InvalidPathException {
    var customPath = getConfig().getCachedDir();
    return customPath != null ? Path.of(customPath) : defaultCachedDir;
  }

  /**
   * System-wide configuration.
   *
   * <p>Stored as JSON in the system config directory.
   */
  public static final class Config {
    private static final Codec<Config> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.optionalFieldOf("sharedDir").forGetter(Config::sharedPath),
                        Codec.STRING.optionalFieldOf("cachedDir").forGetter(Config::cachedPath))
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

    private @Nullable String sharedDir;
    private @Nullable String cachedDir;

    private Config() {
      this.sharedDir = null;
      this.cachedDir = null;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Config(Optional<String> sharedDir, Optional<String> cachedDir) {
      this.sharedDir = sharedDir.orElse(null);
      this.cachedDir = cachedDir.orElse(null);
    }

    private Optional<String> sharedPath() {
      return Optional.ofNullable(sharedDir);
    }

    private Optional<String> cachedPath() {
      return Optional.ofNullable(cachedDir);
    }

    private @Nullable String getSharedDir() {
      return sharedDir;
    }

    private @Nullable String getCachedDir() {
      return cachedDir;
    }

    /**
     * Sets the custom shared content directory path.
     *
     * @param sharedDir the custom path, or null to use default
     */
    public void setSharedDir(@Nullable String sharedDir) {
      this.sharedDir = sharedDir;
    }

    /**
     * Sets the custom cached content directory path.
     *
     * @param cachedDir the custom path, or null to use default
     */
    public void setCachedDir(@Nullable String cachedDir) {
      this.cachedDir = cachedDir;
    }
  }
}
