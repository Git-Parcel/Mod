package io.github.leawind.gitparcel.common.impl.extension.transientfield;

import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientFieldRegistry;
import java.util.ArrayList;
import java.util.List;

public final class ParcelTransientFieldRegistryImpl implements ParcelTransientFieldRegistry {
  public static final ParcelTransientFieldRegistryImpl INSTANCE =
      new ParcelTransientFieldRegistryImpl();

  private final List<ParcelTransientField> fields = new ArrayList<>();
  private boolean frozen;

  private ParcelTransientFieldRegistryImpl() {}

  @Override
  public void register(ParcelTransientField field) {
    if (frozen) {
      throw new IllegalStateException("Parcel transient field registry is frozen");
    }
    if (!fields.contains(field)) {
      fields.add(field);
    }
  }

  @Override
  public List<ParcelTransientField> fields() {
    return List.copyOf(fields);
  }

  @Override
  public void freeze() {
    frozen = true;
  }

  @Override
  public boolean isFrozen() {
    return frozen;
  }
}
