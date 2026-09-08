package io.github.leawind.gitparcel.common.api.extension.field;

import java.util.List;

/** Registry of declared NBT coordinate fields, frozen after extension discovery. */
public interface ParcelCoordinateFieldRegistry {
  static ParcelCoordinateFieldRegistry get() {
    return io.github.leawind.gitparcel.common.impl.extension.field.ParcelCoordinateFieldRegistryImpl.INSTANCE;
  }

  void register(ParcelCoordinateField field);

  List<ParcelCoordinateField> fields();

  void freeze();

  boolean isFrozen();
}
