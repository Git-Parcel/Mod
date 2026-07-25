package io.github.leawind.gitparcel.common.impl.extension.attachment;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ParcelAttachmentTypeRegistryImpl implements ParcelAttachmentTypeRegistry {
  public static final ParcelAttachmentTypeRegistryImpl INSTANCE =
      new ParcelAttachmentTypeRegistryImpl();

  private final Map<Identifier, ParcelAttachmentType> types = new LinkedHashMap<>();
  private boolean frozen;

  private ParcelAttachmentTypeRegistryImpl() {}

  @Override
  public void register(ParcelAttachmentType type) {
    if (frozen) {
      throw new IllegalStateException("Parcel attachment registry is frozen");
    }
    if (types.putIfAbsent(type.id(), type) != null) {
      throw new IllegalArgumentException("duplicate parcel attachment type: " + type.id());
    }
  }

  @Override
  public @Nullable ParcelAttachmentType get(Identifier id) {
    return types.get(id);
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
