package io.github.leawind.gitparcel.common.api.extension.transientfield;

import java.util.List;

/** Registry of declared transient NBT fields (definition 2.5). */
public interface ParcelTransientFieldRegistry {
  static ParcelTransientFieldRegistry get() {
    return io.github.leawind.gitparcel.common.impl.extension.transientfield.ParcelTransientFieldRegistryImpl.INSTANCE;
  }

  void register(ParcelTransientField field);

  List<ParcelTransientField> fields();

  void freeze();

  boolean isFrozen();
}
