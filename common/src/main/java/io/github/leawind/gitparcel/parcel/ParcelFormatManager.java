package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.parcel.config.ParcelFormatConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class ParcelFormatManager {
  private final Map<String, Map<Integer, ParcelFormat<?>>> savers = new HashMap<>();
  private final Map<String, Map<Integer, ParcelFormat<?>>> loaders = new HashMap<>();
  private ParcelFormat.@Nullable Save<?> defaultSaver;
  private ParcelFormat.@Nullable Load<?> defaultLoader;

  public <C extends ParcelFormatConfig<C>> ParcelFormatManager register(ParcelFormat<C> format) {
    if (format instanceof ParcelFormat.Save<C> saver) {
      savers.computeIfAbsent(format.id(), k -> new HashMap<>()).put(format.version(), saver);
    } else if (format instanceof ParcelFormat.Load<C> loader) {
      loaders.computeIfAbsent(format.id(), k -> new HashMap<>()).put(format.version(), loader);
    }
    return this;
  }

  public <C extends ParcelFormatConfig<C>> ParcelFormatManager registerDefault(
      ParcelFormat<C> format) {
    if (format instanceof ParcelFormat.Save<C> saver) {
      defaultSaver = saver;
    } else if (format instanceof ParcelFormat.Load<C> loader) {
      defaultLoader = loader;
    }
    return register(format);
  }

  /**
   * Get the default saver.
   *
   * @throws NullPointerException if no default saver is set
   */
  public ParcelFormat.Save<?> defaultSaver() throws NullPointerException {
    return Objects.requireNonNull(defaultSaver);
  }

  /**
   * Get the default loader.
   *
   * @throws NullPointerException if no default loader is set
   */
  public ParcelFormat.Load<?> defaultLoader() throws NullPointerException {
    return Objects.requireNonNull(defaultLoader);
  }

  /**
   * Get the latest version of saver for the given format id.
   *
   * @param id The format id.
   * @return null if no saver is found
   */
  public ParcelFormat.@Nullable Save<?> getSaver(String id) {
    return (ParcelFormat.Save<?>) getLatest(savers, id);
  }

  public ParcelFormat.@Nullable Save<?> getSaver(String id, int version) {
    return (ParcelFormat.Save<?>) savers.getOrDefault(id, Map.of()).get(version);
  }

  /**
   * Get the latest version of loader for the given format id.
   *
   * @param id The format id.
   * @return null if no loader is found
   */
  public ParcelFormat.@Nullable Load<?> getLoader(String id) {
    return (ParcelFormat.Load<?>) getLatest(loaders, id);
  }

  public ParcelFormat.@Nullable Load<?> getLoader(String id, int version) {
    return (ParcelFormat.Load<?>) loaders.getOrDefault(id, Map.of()).get(version);
  }

  public Set<String> getSaverNames() {
    return savers.keySet();
  }

  public Set<String> getLoaderNames() {
    return loaders.keySet();
  }

  private static @Nullable ParcelFormat<?> getLatest(
      Map<String, Map<Integer, ParcelFormat<?>>> formats, String id) {
    var versions = formats.get(id);
    if (versions == null) {
      return null;
    }
    var sorted =
        versions //
            .values()
            .stream()
            .sorted((a, b) -> b.version() - a.version())
            .toList();
    return sorted.getFirst();
  }
}
