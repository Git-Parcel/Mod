package io.github.leawind.gitparcel.api.parcel;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * A registry for {@link ParcelFormat} savers and loaders.
 *
 * <p>Maintains separate maps for save and load format implementations, keyed by {@link
 * ParcelFormat.Info}. Also tracks an optional default saver used when no specific format is
 * requested.
 *
 * <p>A singleton instance is available via {@link #INSTANCE}, though subclasses may create
 * additional registries as needed.
 */
public final class ParcelFormatRegistry {
  /** The global singleton instance of {@code ParcelFormatRegistry}. */
  public static final ParcelFormatRegistry INSTANCE = new ParcelFormatRegistry();

  private final Map<ParcelFormat.Info, ParcelFormat.Save<?>> savers = new Object2ObjectArrayMap<>();
  private final Map<ParcelFormat.Info, ParcelFormat.Load<?>> loaders =
      new Object2ObjectArrayMap<>();

  private ParcelFormat.@Nullable Save<?> defaultSaver;

  /**
   * Constructs a new, empty {@code ParcelFormatRegistry}.
   *
   * <p>Protected to allow subclassing while discouraging direct instantiation in favor of {@link
   * #INSTANCE}.
   */
  private ParcelFormatRegistry() {}

  /** Clears all registered formats. */
  public void clear() {
    savers.clear();
    loaders.clear();
    defaultSaver = null;
  }

  /**
   * Registers a format implementation as either a saver or a loader.
   *
   * <p>The format must implement exactly one of {@link ParcelFormat.Save} or {@link
   * ParcelFormat.Load}. Registering the same {@link ParcelFormat.Info} twice for the same role is
   * not allowed.
   *
   * @param <C> the config type associated with the format
   * @param format the format implementation to register
   * @throws IllegalArgumentException if {@code format} is neither a saver nor a loader, or if a
   *     saver or loader with the same {@link ParcelFormat.Info} is already registered
   */
  public <C extends ParcelFormatConfig<C>> void register(ParcelFormat.Impl<C> format)
      throws IllegalArgumentException {

    boolean isSaverOrLoader = false;

    if (format instanceof ParcelFormat.Save<?> saver) {
      if (savers.containsKey(saver.info())) {
        throw new IllegalArgumentException("duplicate saver: " + saver);
      }
      savers.put(format.info(), saver);
      isSaverOrLoader = true;
    }

    if (format instanceof ParcelFormat.Load<?> loader) {
      if (loaders.containsKey(loader.info())) {
        throw new IllegalArgumentException("duplicate loader: " + loader);
      }
      loaders.put(format.info(), loader);
      isSaverOrLoader = true;
    }

    if (!isSaverOrLoader) {
      throw new IllegalArgumentException("format must be either saver or loader");
    }
  }

  /**
   * Registers a saver and designates it as the default saver.
   *
   * <p>The format is first registered via {@link #register}, then stored as the default saver
   * returned by {@link #defaultSaver()}.
   *
   * @param <C> the config type associated with the format
   * @param format the saver to register as the default
   * @throws IllegalArgumentException if the format is already registered as a saver
   */
  public <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Save<C> format)
      throws IllegalArgumentException {
    register(format);
    defaultSaver = format;
  }

  /**
   * Returns the default saver.
   *
   * @return the default {@link ParcelFormat.Save} instance
   * @throws NullPointerException if no default saver has been set via {@link #registerDefaultSaver}
   */
  public ParcelFormat.Save<?> defaultSaver() throws NullPointerException {
    return Objects.requireNonNull(defaultSaver);
  }

  /**
   * Returns the highest-versioned registered saver for the given format id.
   *
   * <p>If multiple savers share the same id (differing by version), only the one with the greatest
   * version number is returned.
   *
   * @param id the format id to look up
   * @return the latest-version saver for {@code id}, or {@code null} if none is registered
   */
  public ParcelFormat.@Nullable Save<?> getSaver(String id) {
    return savers.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  /**
   * Returns the registered saver for the given {@link ParcelFormat.Info}.
   *
   * @param info the exact info key (id + version) to look up
   * @return the matching saver, or {@code null} if none is registered for {@code info}
   */
  public ParcelFormat.@Nullable Save<?> getSaver(ParcelFormat.Info info) {
    return savers.get(info);
  }

  /**
   * Returns the highest-versioned registered loader for the given format id.
   *
   * <p>If multiple loaders share the same id (differing by version), only the one with the greatest
   * version number is returned.
   *
   * @param id the format id to look up
   * @return the latest-version loader for {@code id}, or {@code null} if none is registered
   */
  public ParcelFormat.@Nullable Load<?> getLoader(String id) {
    return loaders.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  /**
   * Returns the registered loader for the given {@link ParcelFormat.Info}.
   *
   * @param info the exact info key (id + version) to look up
   * @return the matching loader, or {@code null} if none is registered for {@code info}
   */
  public ParcelFormat.@Nullable Load<?> getLoader(ParcelFormat.Info info) {
    return loaders.get(info);
  }

  /**
   * Returns the set of all registered saver format ids.
   *
   * @return an unordered {@link Set} of saver id strings; empty if no savers are registered
   */
  public Set<String> getSaverNames() {
    return savers.keySet().stream().map(ParcelFormat.Info::id).collect(Collectors.toSet());
  }

  /**
   * Returns the set of all registered loader format ids.
   *
   * @return an unordered {@link Set} of loader id strings; empty if no loaders are registered
   */
  public Set<String> getLoaderNames() {
    return loaders.keySet().stream().map(ParcelFormat.Info::id).collect(Collectors.toSet());
  }

  public List<ParcelFormat.Info> getSaverInfos() {
    return new ArrayList<>(savers.keySet());
  }

  public List<ParcelFormat.Info> getLoaderInfos() {
    return new ArrayList<>(loaders.keySet());
  }
}
