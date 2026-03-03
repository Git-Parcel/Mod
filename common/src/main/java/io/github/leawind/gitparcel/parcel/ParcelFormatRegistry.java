package io.github.leawind.gitparcel.parcel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class ParcelFormatRegistry {
  private final Map<String, Map<Integer, ParcelFormat<?>>> savers = new HashMap<>();
  private final Map<String, Map<Integer, ParcelFormat<?>>> loaders = new HashMap<>();
  private ParcelFormat.@Nullable Save<?> defaultSaver;

  /**
   * Registers a new format.
   *
   * @param format The format to register.
   * @param <C> The format config type.
   */
  public <C extends ParcelFormatConfig<C>> void register(ParcelFormat<C> format) {

    boolean isSaverOrLoader = false;

    if (format instanceof ParcelFormat.Save<C> saver) {
      registerSaver(saver);
      isSaverOrLoader = true;
    }

    if (format instanceof ParcelFormat.Load<C> loader) {
      registerLoader(loader);
      isSaverOrLoader = true;
    }

    if (!isSaverOrLoader) {
      throw new IllegalArgumentException(
          "Expected a saver or loader, got " + format.getClass().getSimpleName());
    }
  }

  public <C extends ParcelFormatConfig<C>> void registerSaver(ParcelFormat.Save<C> format) {
    savers.computeIfAbsent(format.id(), k -> new HashMap<>()).put(format.version(), format);
  }

  public <C extends ParcelFormatConfig<C>> void registerLoader(ParcelFormat.Load<C> format) {
    loaders.computeIfAbsent(format.id(), k -> new HashMap<>()).put(format.version(), format);
  }

  public <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Save<C> format) {
    register(format);
    defaultSaver = format;
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
