package io.github.leawind.gitparcel.common.api.parcel;

import io.github.leawind.gitparcel.common.api.Factory;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * A registry for {@link ParcelFormat} writers and readers.
 *
 * <p>Maintains separate maps for writer and reader implementations, keyed by {@link
 * ParcelFormat.Spec}. Also tracks an optional default writer used when no specific format is
 * requested.
 *
 * <p>A singleton instance is available via {@link #get()}, though subclasses may create additional
 * registries as needed.
 */
public interface ParcelFormatRegistry {

  static ParcelFormatRegistry get() {
    return Factory.getParcelFormatRegistry();
  }

  void clear();

  /** Prevents further registrations. */
  void freeze();

  boolean isFrozen();

  /**
   * @throws IllegalArgumentException if {@code format} is neither a writer nor a reader, or if a
   *     writer or reader with the same {@link ParcelFormat.Spec} is already registered
   */
  <C extends ParcelFormatConfig<C>, F extends ParcelFormat.Impl<C>> void register(F format);

  /**
   * @throws IllegalArgumentException if the format is already registered as a writer
   */
  <C extends ParcelFormatConfig<C>> void registerDefaultWriter(ParcelFormat.Writer<C> format);

  /**
   * Selects an already registered writer as the default.
   *
   * @throws IllegalStateException if no writer is registered for {@code spec}
   */
  void setDefaultWriter(ParcelFormat.Spec spec);

  /**
   * Returns the default writer.
   *
   * @return the default {@link ParcelFormat.Writer} instance
   * @throws NullPointerException if no default writer has been selected
   */
  ParcelFormat.Writer<?> defaultWriter();

  /**
   * @return the latest-version writer for {@code id}, or {@code null} if none is registered
   */
  ParcelFormat.@Nullable Writer<?> getWriter(String id);

  ParcelFormat.@Nullable Writer<?> getWriter(ParcelFormat.Spec spec);

  /**
   * @return the latest-version reader for {@code id}, or {@code null} if none is registered
   */
  ParcelFormat.@Nullable Reader<?> getReader(String id);

  ParcelFormat.@Nullable Reader<?> getReader(ParcelFormat.Spec spec);

  /**
   * @return an unordered {@link Set} of writer id strings; empty if no writers are registered
   */
  Set<String> getWriterNames();

  /**
   * @return an unordered {@link Set} of reader id strings; empty if no readers are registered
   */
  Set<String> getReaderNames();

  Stream<ParcelFormat.Writer<?>> streamWriters();

  Stream<ParcelFormat.Reader<?>> streamReaders();
}
