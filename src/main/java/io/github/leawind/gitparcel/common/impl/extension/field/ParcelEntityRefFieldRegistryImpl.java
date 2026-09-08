package io.github.leawind.gitparcel.common.impl.extension.field;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import java.util.ArrayList;
import java.util.List;

public final class ParcelEntityRefFieldRegistryImpl implements ParcelEntityRefFieldRegistry {
  public static final ParcelEntityRefFieldRegistryImpl INSTANCE =
      new ParcelEntityRefFieldRegistryImpl();

  private final List<ParcelEntityRefField> fields = new ArrayList<>();
  private boolean frozen;

  private ParcelEntityRefFieldRegistryImpl() {}

  @Override
  public void register(ParcelEntityRefField field) {
    if (frozen) {
      throw new IllegalStateException("Parcel entity reference field registry is frozen");
    }
    fields.add(field);
  }

  @Override
  public List<ParcelEntityRefField> fields() {
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
