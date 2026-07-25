package io.github.leawind.gitparcel.common.impl.parcel;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public final class ParcelFormatRegistryImpl implements ParcelFormatRegistry {
  private ParcelFormatRegistryImpl() {}

  public static final ParcelFormatRegistry INSTANCE = new ParcelFormatRegistryImpl();

  private final Map<ParcelFormat.Spec, ParcelFormat.Writer<?>> writers =
      new Object2ObjectArrayMap<>();
  private final Map<ParcelFormat.Spec, ParcelFormat.Reader<?>> readers =
      new Object2ObjectArrayMap<>();

  private ParcelFormat.@Nullable Writer<?> defaultWriter;
  private boolean frozen;

  public void clear() {
    ensureMutable();
    writers.clear();
    readers.clear();
    defaultWriter = null;
  }

  public <C extends ParcelFormatConfig<C>, F extends ParcelFormat.Impl<C>> void register(F format)
      throws IllegalArgumentException {
    ensureMutable();

    boolean isWriterOrReader = false;

    if (format instanceof ParcelFormat.Writer<?> writer) {
      if (writers.containsKey(writer.spec())) {
        throw new IllegalArgumentException("duplicate writer: " + writer);
      }
      writers.put(format.spec(), writer);
      isWriterOrReader = true;
    }

    if (format instanceof ParcelFormat.Reader<?> reader) {
      if (readers.containsKey(reader.spec())) {
        throw new IllegalArgumentException("duplicate reader: " + reader);
      }
      readers.put(format.spec(), reader);
      isWriterOrReader = true;
    }

    if (!isWriterOrReader) {
      throw new IllegalArgumentException("format must be either writer or reader");
    }
  }

  public <C extends ParcelFormatConfig<C>> void registerDefaultWriter(ParcelFormat.Writer<C> format)
      throws IllegalArgumentException {
    ensureMutable();
    register(format);
    defaultWriter = format;
  }

  @Override
  public void setDefaultWriter(ParcelFormat.Spec spec) {
    ensureMutable();
    var writer = getWriter(spec);
    if (writer == null) {
      throw new IllegalStateException("Default parcel format is not registered: " + spec);
    }
    defaultWriter = writer;
  }

  @Override
  public void freeze() {
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }

  private void ensureMutable() {
    if (frozen) {
      throw new IllegalStateException("Parcel format registry is frozen");
    }
  }

  public ParcelFormat.Writer<?> defaultWriter() throws NullPointerException {
    return Objects.requireNonNull(defaultWriter);
  }

  public ParcelFormat.@Nullable Writer<?> getWriter(String id) {
    return writers.values().stream()
        .filter(format -> format.spec().id().equals(id))
        .max(Comparator.comparingInt(f -> f.spec().version()))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Writer<?> getWriter(ParcelFormat.Spec spec) {
    return writers.get(spec);
  }

  public ParcelFormat.@Nullable Reader<?> getReader(String id) {
    return readers.values().stream()
        .filter(format -> format.spec().id().equals(id))
        .max(Comparator.comparingInt(f -> f.spec().version()))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Reader<?> getReader(ParcelFormat.Spec spec) {
    return readers.get(spec);
  }

  public Set<String> getWriterNames() {
    return writers.keySet().stream().map(ParcelFormat.Spec::id).collect(Collectors.toSet());
  }

  public Set<String> getReaderNames() {
    return readers.keySet().stream().map(ParcelFormat.Spec::id).collect(Collectors.toSet());
  }

  public Stream<ParcelFormat.Writer<?>> streamWriters() {
    return writers.values().stream();
  }

  public Stream<ParcelFormat.Reader<?>> streamReaders() {
    return readers.values().stream();
  }
}
