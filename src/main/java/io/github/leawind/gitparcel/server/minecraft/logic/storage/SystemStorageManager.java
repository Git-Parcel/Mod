package io.github.leawind.gitparcel.server.minecraft.logic.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.GitParcel;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;

/**
 * Manages system-level storage via SystemStorageLib.
 *
 * <p>System storage is shared across all game instances and includes:
 *
 * <ul>
 *   <li>SystemConfig - System-wide configuration (sharedPath, etc.)
 *   <li>Secrets - Encrypted sensitive credentials (Access Tokens, OAuth tokens)
 *   <li>System Shared - Shareable content when system storage is enabled
 * </ul>
 */
public class SystemStorageManager {
  private static final Scope SCOPE = SystemStorageLib.getInstance().scope(GitParcel.MOD_ID);

  /** Configuration file name. */
  private static final String CONFIG_FILE_NAME = "system_config.json";

  /** Gson instance configured for pretty-printed JSON output. */
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static @Nullable Config config;

  public static Path getDataDir() {
    return SCOPE.directory(StoreType.DATA);
  }

  public static Path getCacheDir() {
    return SCOPE.directory(StoreType.CACHE);
  }

  public static Path getConfigFile() {
    return SCOPE.directory(StoreType.CONFIG).resolve(CONFIG_FILE_NAME);
  }

  public static SecretsAccessor getSecrets() {
    return SCOPE.access(StoreType.SECRETS, SecretsAccessor::from);
  }

  /**
   * Gets the system shared directory, resolved from config.
   *
   * <p>If no config exists, uses the default: {@code StoreType.DATA/shared/}.
   *
   * @return the system shared directory path
   * @throws IOException if the config file exists but cannot be read
   */
  public static Path getSharedDir() throws IOException {
    return Path.of(getConfig().sharedPath());
  }

  /**
   * Gets the system configuration, loading it from disk if necessary.
   *
   * @return the system configuration
   * @throws IOException if the configuration file exists but cannot be loaded
   */
  private static Config getConfig() throws IOException {
    if (config == null) {
      var configFile = getConfigFile();
      if (Files.exists(configFile)) {
        config = Config.load(configFile);
      } else {
        config = Config.createDefault();
      }
    }
    return config;
  }

  /**
   * Saves the current system configuration to disk.
   *
   * @throws IOException if an I/O error occurs
   */
  public static void saveConfig() throws IOException {
    if (config != null) {
      config.save(getConfigFile());
    }
  }

  /**
   * System-wide configuration stored via {@link StoreType#CONFIG}.
   *
   * <p>Stored as {@code system_config.json}, using Codec serialization.
   */
  public static final class Config {
    private static final Codec<Config> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING
                            .optionalFieldOf("sharedPath", defaultSharedPath())
                            .forGetter(Config::sharedPath))
                    .apply(inst, Config::new));

    private static String defaultSharedPath() {
      return SCOPE.directory(StoreType.DATA).resolve("shared").toString();
    }

    /**
     * Loads the config from the specified file.
     *
     * @param file the config file path
     * @return the loaded config
     * @throws IOException if the file cannot be read
     */
    private static Config load(Path file) throws IOException {
      var json = GSON.fromJson(Files.readString(file), JsonObject.class);
      return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private void save(Path file) throws IOException {
      Files.createDirectories(file.getParent());
      var result = CODEC.encodeStart(JsonOps.INSTANCE, this);
      Files.writeString(file, GSON.toJson((JsonObject) result.getOrThrow()));
    }

    private final String sharedPath;

    private Config(String sharedPath) {
      this.sharedPath = sharedPath;
    }

    /**
     * Creates a config with default values.
     *
     * @return a new config with default sharedPath
     */
    static Config createDefault() {
      return new Config(defaultSharedPath());
    }

    /**
     * Gets the absolute path to the system shared directory.
     *
     * @return the shared directory path
     */
    public String sharedPath() {
      return sharedPath;
    }
  }
}
