package io.github.leawind.gitparcel.server.minecraft.logic.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.jspecify.annotations.Nullable;

/**
 * Build-time mod metadata embedded in the jar, written by the Gradle build.
 *
 * <p>Keeps the web console independent of loader-specific version APIs: fabric, NeoForge and
 * Forge all ship the same properties file.
 */
final class WebConsoleMeta {
  private static final String PROPERTIES_PATH = "/gitparcel/gitparcel.properties";

  private static volatile @Nullable Properties cached;

  private WebConsoleMeta() {}

  static String modVersion() {
    var properties = cached;
    if (properties == null) {
      properties = load();
      cached = properties;
    }
    return properties.getProperty("mod.version", "unknown");
  }

  private static Properties load() {
    var properties = new Properties();
    try (InputStream stream =
        WebConsoleMeta.class.getClassLoader().getResourceAsStream(PROPERTIES_PATH)) {
      if (stream != null) {
        properties.load(stream);
      }
    } catch (IOException e) {
      // A missing or unreadable metadata file must not take the console down.
    }
    return properties;
  }
}
