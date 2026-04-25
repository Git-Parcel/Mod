package io.github.leawind.gitparcel.api.parcel;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A registry for {@link ParcelFormat} savers and loaders.
 *
 * <p>Maintains separate maps for save and load format implementations, keyed by {@link
 * ParcelFormat.Spec}. Also tracks an optional default saver used when no specific format is
 * requested.
 *
 * <p>A singleton instance is available via {@link #INSTANCE}, though subclasses may create
 * additional registries as needed.
 */
public final class ParcelFormatRegistry {
  public static final ParcelFormatRegistry INSTANCE = new ParcelFormatRegistry();

  private final Map<ParcelFormat.Spec, ParcelFormat.Saver<?>> savers =
      new Object2ObjectArrayMap<>();
  private final Map<ParcelFormat.Spec, ParcelFormat.Loader<?>> loaders =
      new Object2ObjectArrayMap<>();

  private ParcelFormat.@Nullable Saver<?> defaultSaver;

  private ParcelFormatRegistry() {}

  public void clear() {
    savers.clear();
    loaders.clear();
    defaultSaver = null;
  }

  /**
   * @throws IllegalArgumentException if {@code format} is neither a saver nor a loader, or if a
   *     saver or loader with the same {@link ParcelFormat.Spec} is already registered
   */
  public <C extends ParcelFormatConfig<C>> void register(ParcelFormat.Impl<C> format)
      throws IllegalArgumentException {

    boolean isSaverOrLoader = false;

    if (format instanceof ParcelFormat.Saver<?> saver) {
      if (savers.containsKey(saver.spec())) {
        throw new IllegalArgumentException("duplicate saver: " + saver);
      }
      savers.put(format.spec(), saver);
      isSaverOrLoader = true;
    }

    if (format instanceof ParcelFormat.Loader<?> loader) {
      if (loaders.containsKey(loader.spec())) {
        throw new IllegalArgumentException("duplicate loader: " + loader);
      }
      loaders.put(format.spec(), loader);
      isSaverOrLoader = true;
    }

    if (!isSaverOrLoader) {
      throw new IllegalArgumentException("format must be either saver or loader");
    }
  }

  /**
   * @throws IllegalArgumentException if the format is already registered as a saver
   */
  public <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Saver<C> format)
      throws IllegalArgumentException {
    register(format);
    defaultSaver = format;
  }

  /**
   * Returns the default saver.
   *
   * @return the default {@link ParcelFormat.Saver} instance
   * @throws NullPointerException if no default saver has been set via {@link #registerDefaultSaver}
   */
  public ParcelFormat.Saver<?> defaultSaver() throws NullPointerException {
    return Objects.requireNonNull(defaultSaver);
  }

  /**
   * @return the latest-version saver for {@code id}, or {@code null} if none is registered
   */
  public ParcelFormat.@Nullable Saver<?> getSaver(String id) {
    return savers.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Saver<?> getSaver(ParcelFormat.Spec spec) {
    return savers.get(spec);
  }

  /**
   * @return the latest-version loader for {@code id}, or {@code null} if none is registered
   */
  public ParcelFormat.@Nullable Loader<?> getLoader(String id) {
    return loaders.values().stream()
        .filter(format -> format.id().equals(id))
        .max(Comparator.comparingInt(ParcelFormat.Impl::version))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Loader<?> getLoader(ParcelFormat.Spec spec) {
    return loaders.get(spec);
  }

  /**
   * Returns the set of all registered saver format ids.
   *
   * @return an unordered {@link Set} of saver id strings; empty if no savers are registered
   */
  public Set<String> getSaverNames() {
    return savers.keySet().stream().map(ParcelFormat.Spec::id).collect(Collectors.toSet());
  }

  /**
   * Returns the set of all registered loader format ids.
   *
   * @return an unordered {@link Set} of loader id strings; empty if no loaders are registered
   */
  public Set<String> getLoaderNames() {
    return loaders.keySet().stream().map(ParcelFormat.Spec::id).collect(Collectors.toSet());
  }

  public Stream<ParcelFormat.Saver<?>> streamSavers() {
    return savers.values().stream();
  }

  public Stream<ParcelFormat.Loader<?>> streamLoaders() {
    return loaders.values().stream();
  }
}
