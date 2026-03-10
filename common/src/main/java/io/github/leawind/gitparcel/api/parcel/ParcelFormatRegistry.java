package io.github.leawind.gitparcel.api.parcel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class ParcelFormatRegistry {
  private final Map<ParcelFormat.Info, ParcelFormat.Save<?>> savers = new HashMap<>();
  private final Map<ParcelFormat.Info, ParcelFormat.Load<?>> loaders = new HashMap<>();

  private ParcelFormat.@Nullable Save<?> defaultSaver;

  /**
   * Register a format.
   *
   * @param format The format to register.
   * @param <C> The config type of the format.
   * @throws IllegalArgumentException if the format is not a saver or loader, or if it is a
   *     duplicate.
   */
  public <C extends ParcelFormatConfig<C>> void register(ParcelFormat.Impl<C> format)
      throws IllegalArgumentException {
    switch (format) {
      case ParcelFormat.Save<?> saver -> {
        if (savers.containsKey(saver.info())) {
          throw new IllegalArgumentException("duplicate saver: " + saver);
        }
        savers.put(format.info(), saver);
      }
      case ParcelFormat.Load<?> loader -> {
        if (loaders.containsKey(loader.info())) {
          throw new IllegalArgumentException("duplicate loader: " + loader);
        }
        loaders.put(format.info(), loader);
      }
      default -> throw new IllegalArgumentException("format must be either saver or loader");
    }
  }

  /**
   * Register the default saver.
   *
   * @throws IllegalArgumentException if the format is not a saver, or if it is not registered.
   */
  public <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Save<C> format)
      throws IllegalArgumentException {
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
    return savers.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Save<?> getSaver(ParcelFormat.Info info) {
    return savers.get(info);
  }

  public ParcelFormat.@Nullable Load<?> getLoader(String id) {
    return loaders.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Load<?> getLoader(ParcelFormat.Info info) {
    return loaders.get(info);
  }

  public Set<String> getSaverNames() {
    return savers.keySet().stream().map(ParcelFormat.Info::id).collect(Collectors.toSet());
  }

  public Set<String> getLoaderNames() {
    return loaders.keySet().stream().map(ParcelFormat.Info::id).collect(Collectors.toSet());
  }
}
