package io.github.leawind.gitparcel.common.impl.extension.attachment;

import io.github.leawind.gitparcel.common.api.extension.RegistrationSource;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParcelAttachmentTypeRegistryImpl implements ParcelAttachmentTypeRegistry {
  public static final ParcelAttachmentTypeRegistryImpl INSTANCE =
      new ParcelAttachmentTypeRegistryImpl();
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ParcelAttachmentTypeRegistryImpl.class);

  private record Registration(RegistrationSource source, ParcelAttachmentType type) {}

  private final Map<Identifier, Registration> registrations = new LinkedHashMap<>();
  private boolean frozen;

  private ParcelAttachmentTypeRegistryImpl() {}

  @Override
  public void register(RegistrationSource source, ParcelAttachmentType type) {
    if (frozen) {
      throw new IllegalStateException("Parcel attachment registry is frozen");
    }
    var existing = registrations.get(type.id());
    if (existing != null && !source.supersedes(existing.source())) {
      LOGGER.warn(
          "Parcel attachment type {} from extension {} was superseded by {} (rule 7.4)",
          type.id(),
          source.extensionId(),
          existing.source().extensionId());
      return;
    }
    if (existing != null) {
      LOGGER.warn(
          "Parcel attachment type {} from extension {} supersedes {} (rule 7.4)",
          type.id(),
          source.extensionId(),
          existing.source().extensionId());
    }
    registrations.put(type.id(), new Registration(source, type));
  }

  @Override
  public @Nullable ParcelAttachmentType get(Identifier id) {
    var registration = registrations.get(id);
    return registration == null ? null : registration.type();
  }

  @Override
  public List<ParcelAttachmentType> types() {
    return registrations.values().stream().map(Registration::type).toList();
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
