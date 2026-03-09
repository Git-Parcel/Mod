package io.github.leawind.gitparcel.api.parcel;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class ParcelFormatRegistry {
  private final Map<String, Int2ObjectSortedMap<ParcelFormat.Impl<?>>> savers = new HashMap<>();
  private final Map<String, Int2ObjectSortedMap<ParcelFormat.Impl<?>>> loaders = new HashMap<>();
  private ParcelFormat.@Nullable Save<?> defaultSaver;

  /**
   * Registers a new format.
   *
   * @param format The format to register.
   * @param <C> The format config type.
   */
  public <C extends ParcelFormatConfig<C>> void register(ParcelFormat.Impl<C> format) {

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
    savers
        .computeIfAbsent(format.id(), k -> new Int2ObjectAVLTreeMap<>())
        .put(format.version(), format);
  }

  public <C extends ParcelFormatConfig<C>> void registerLoader(ParcelFormat.Load<C> format) {
    loaders
        .computeIfAbsent(format.id(), k -> new Int2ObjectAVLTreeMap<>())
        .put(format.version(), format);
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
    return (ParcelFormat.Save<?>)
        savers.getOrDefault(id, new Int2ObjectAVLTreeMap<>()).get(version);
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
    return (ParcelFormat.Load<?>)
        loaders.getOrDefault(id, new Int2ObjectAVLTreeMap<>()).get(version);
  }

  public Set<String> getSaverNames() {
    return savers.keySet();
  }

  public Set<String> getLoaderNames() {
    return loaders.keySet();
  }

  private static ParcelFormat.@Nullable Impl<?> getLatest(
      Map<String, Int2ObjectSortedMap<ParcelFormat.Impl<?>>> formats, String id) {

    var versions = formats.get(id);

    if (versions == null) {
      return null;
    }

    return versions.sequencedValues().getLast();
  }
}
