package io.github.leawind.gitparcel.common.api.extension.field;

import java.util.List;

/** Registry of declared entity-reference fields, frozen after extension discovery. */
public interface ParcelEntityRefFieldRegistry {
  static ParcelEntityRefFieldRegistry get() {
    return io.github.leawind.gitparcel.common.impl.extension.field.ParcelEntityRefFieldRegistryImpl.INSTANCE;
  }

  void register(ParcelEntityRefField field);

  List<ParcelEntityRefField> fields();

  void freeze();

  boolean isFrozen();
}
