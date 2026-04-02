package io.github.leawind.gitparcel.server.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Manages sensitive data such as Access Tokens and OAuth refresh tokens. */
public final class SecretManager {
  private static final String SECRETS_FILE = "secrets.properties";
  private static final String COMMENTS = "GitParcel Secrets";

  private final Path root;
  private final Path filePath;

  /**
   * Creates a SecretManager for the specified directory.
   *
   * @param root the directory to store secrets
   */
  public SecretManager(Path root) {
    this.root = root;
    this.filePath = root.resolve(SECRETS_FILE);
  }

  /**
   * Gets the secret directory path.
   *
   * @return the secret directory path
   */
  public Path getRoot() {
    return root;
  }

  /** Loads the secrets file into the provided properties. */
  private Properties loadProperties(Properties properties) throws IOException {
    if (!Files.exists(filePath)) {
      return properties;
    }
    try (var reader = Files.newBufferedReader(filePath)) {
      properties.load(reader);
    }
    return properties;
  }

  /**
   * Tries to load the secrets file.
   *
   * <ul>
   *   <li>If the file does not exist, return null.
   *   <li>If the file cannot be loaded, return null.
   *   <li>If the file can be loaded, return the properties.
   * </ul>
   */
  private @Nullable Properties tryLoadProperties() {
    if (!Files.exists(filePath)) {
      return null;
    }

    var properties = new Properties();
    try (var reader = Files.newBufferedReader(filePath)) {
      properties.load(reader);
    } catch (IOException e) {
      return null;
    }
    return properties;
  }

  /**
   * Gets a secret value by key.
   *
   * @param key the secret key
   * @return the secret value, or null if not found
   */
  public @Nullable String get(String key) {
    var properties = tryLoadProperties();

    if (properties == null) {
      return null;
    }

    return properties.getProperty(key);
  }

  /**
   * Sets a secret value.
   *
   * @param key the secret key
   * @param value the secret value
   * @throws IOException if the secret cannot be saved
   */
  public void put(String key, String value) throws IOException {
    Files.createDirectories(root);

    var properties = loadProperties(new Properties());
    properties.setProperty(key, value);

    try (var writer = Files.newBufferedWriter(filePath)) {
      properties.store(writer, COMMENTS);
    }
  }

  /**
   * Removes a secret.
   *
   * @param key the secret key to remove
   * @throws IOException if the secret cannot be removed
   */
  public void remove(String key) throws IOException {
    var properties = loadProperties(new Properties());

    properties.remove(key);

    try (var writer = Files.newBufferedWriter(filePath)) {
      properties.store(writer, COMMENTS);
    }
  }

  /**
   * Gets all secret keys.
   *
   * @return a set of all secret keys
   */
  public Set<String> keySet() {
    var properties = tryLoadProperties();
    if (properties == null) {
      return Set.of();
    }

    return properties.stringPropertyNames();
  }
}
