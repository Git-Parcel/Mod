package io.github.leawind.gitparcel.common.impl.extension;

import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ParcelExtensionRegistrarImpl implements ParcelExtensionRegistrar {
  private final List<ParcelFormat.Impl<?>> formats = new ArrayList<>();

  @Override
  public void registerFormat(ParcelFormat.Impl<?> format) {
    formats.add(format);
  }

  void commit(ParcelFormatRegistry registry) {
    Set<ParcelFormat.Spec> saverSpecs = new HashSet<>();
    Set<ParcelFormat.Spec> loaderSpecs = new HashSet<>();

    for (var format : formats) {
      boolean valid = false;
      if (format instanceof ParcelFormat.Saver<?> saver) {
        valid = true;
        if (!saverSpecs.add(saver.spec()) || registry.getSaver(saver.spec()) != null) {
          throw new IllegalArgumentException("duplicate saver: " + saver.spec());
        }
      }
      if (format instanceof ParcelFormat.Loader<?> loader) {
        valid = true;
        if (!loaderSpecs.add(loader.spec()) || registry.getLoader(loader.spec()) != null) {
          throw new IllegalArgumentException("duplicate loader: " + loader.spec());
        }
      }
      if (!valid) {
        throw new IllegalArgumentException("format must be either saver or loader: " + format);
      }
    }

    for (var format : formats) {
      registerUnchecked(registry, format);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void registerUnchecked(
      ParcelFormatRegistry registry, ParcelFormat.Impl<?> format) {
    registry.register((ParcelFormat.Impl) format);
  }
}
