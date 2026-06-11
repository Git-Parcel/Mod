package io.github.leawind.gitparcel.core.api.parcel;

import io.github.leawind.gitparcel.core.api.Bridge;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A registry for {@link ParcelFormat} savers and loaders.
 *
 * <p>Maintains separate maps for save and load format implementations, keyed by {@link
 * ParcelFormat.Spec}. Also tracks an optional default saver used when no specific format is
 * requested.
 *
 * <p>A singleton instance is available via {@link #get()}, though subclasses may create additional
 * registries as needed.
 */
public interface ParcelFormatRegistry {

  static ParcelFormatRegistry get() {
    return Bridge.getParcelFormatRegistry();
  }

  void clear();

  /**
   * @throws IllegalArgumentException if {@code format} is neither a saver nor a loader, or if a
   *     saver or loader with the same {@link ParcelFormat.Spec} is already registered
   */
  <C extends ParcelFormatConfig<C>, F extends ParcelFormat.Impl<C>> void register(F format);

  /**
   * @throws IllegalArgumentException if the format is already registered as a saver
   */
  <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Saver<C> format);

  /**
   * Returns the default saver.
   *
   * @return the default {@link ParcelFormat.Saver} instance
   * @throws NullPointerException if no default saver has been set via {@link #registerDefaultSaver}
   */
  ParcelFormat.Saver<?> defaultSaver();

  /**
   * @return the latest-version saver for {@code id}, or {@code null} if none is registered
   */
  ParcelFormat.@Nullable Saver<?> getSaver(String id);

  ParcelFormat.@Nullable Saver<?> getSaver(ParcelFormat.Spec spec);

  /**
   * @return the latest-version loader for {@code id}, or {@code null} if none is registered
   */
  ParcelFormat.@Nullable Loader<?> getLoader(String id);

  ParcelFormat.@Nullable Loader<?> getLoader(ParcelFormat.Spec spec);

  /**
   * @return an unordered {@link Set} of saver id strings; empty if no savers are registered
   */
  Set<String> getSaverNames();

  /**
   * @return an unordered {@link Set} of loader id strings; empty if no loaders are registered
   */
  Set<String> getLoaderNames();

  Stream<ParcelFormat.Saver<?>> streamSavers();

  Stream<ParcelFormat.Loader<?>> streamLoaders();
}
