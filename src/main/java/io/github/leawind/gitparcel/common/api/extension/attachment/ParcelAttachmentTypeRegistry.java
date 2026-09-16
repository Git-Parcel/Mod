package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentTypeRegistryImpl;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface ParcelAttachmentTypeRegistry {
  static ParcelAttachmentTypeRegistry get() {
    return ParcelAttachmentTypeRegistryImpl.INSTANCE;
  }

  void register(io.github.leawind.gitparcel.common.api.extension.RegistrationSource source,
      ParcelAttachmentType type);

  @Nullable ParcelAttachmentType get(Identifier id);

  /** All registered attachment types in registration order. */
  java.util.List<ParcelAttachmentType> types();

  void freeze();

  boolean isFrozen();
}
