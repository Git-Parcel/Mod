package io.github.leawind.gitparcel.common.impl.extension.field;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import java.util.ArrayList;
import java.util.List;

public final class ParcelCoordinateFieldRegistryImpl implements ParcelCoordinateFieldRegistry {
  public static final ParcelCoordinateFieldRegistryImpl INSTANCE =
      new ParcelCoordinateFieldRegistryImpl();

  private final List<ParcelCoordinateField> fields = new ArrayList<>();
  private boolean frozen;

  private ParcelCoordinateFieldRegistryImpl() {}

  @Override
  public void register(ParcelCoordinateField field) {
    if (frozen) {
      throw new IllegalStateException("Parcel coordinate field registry is frozen");
    }
    fields.add(field);
  }

  @Override
  public List<ParcelCoordinateField> fields() {
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
