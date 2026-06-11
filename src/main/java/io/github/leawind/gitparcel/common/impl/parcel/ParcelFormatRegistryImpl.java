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

  private final Map<ParcelFormat.Spec, ParcelFormat.Saver<?>> savers =
      new Object2ObjectArrayMap<>();
  private final Map<ParcelFormat.Spec, ParcelFormat.Loader<?>> loaders =
      new Object2ObjectArrayMap<>();

  private ParcelFormat.@Nullable Saver<?> defaultSaver;

  public void clear() {
    savers.clear();
    loaders.clear();
    defaultSaver = null;
  }

  public <C extends ParcelFormatConfig<C>, F extends ParcelFormat.Impl<C>> void register(F format)
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

  public <C extends ParcelFormatConfig<C>> void registerDefaultSaver(ParcelFormat.Saver<C> format)
      throws IllegalArgumentException {
    register(format);
    defaultSaver = format;
  }

  public ParcelFormat.Saver<?> defaultSaver() throws NullPointerException {
    return Objects.requireNonNull(defaultSaver);
  }

  public ParcelFormat.@Nullable Saver<?> getSaver(String id) {
    return savers.values().stream()
        .filter(format -> format.spec().id().equals(id))
        .max(Comparator.comparingInt(f -> f.spec().version()))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Saver<?> getSaver(ParcelFormat.Spec spec) {
    return savers.get(spec);
  }

  public ParcelFormat.@Nullable Loader<?> getLoader(String id) {
    return loaders.values().stream()
        .filter(format -> format.spec().id().equals(id))
        .max(Comparator.comparingInt(f -> f.spec().version()))
        .orElse(null);
  }

  public ParcelFormat.@Nullable Loader<?> getLoader(ParcelFormat.Spec spec) {
    return loaders.get(spec);
  }

  public Set<String> getSaverNames() {
    return savers.keySet().stream().map(ParcelFormat.Spec::id).collect(Collectors.toSet());
  }

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
