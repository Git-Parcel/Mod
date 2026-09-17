package io.github.leawind.gitparcel.common.api.extension;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributor;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;

/** Registration surface exposed to a {@link GitParcelExtension}. */
public interface ParcelExtensionRegistrar {
  /** Registers a versioned parcel content type. */
  void registerContentType(ParcelContentType<?> type);

  void registerProcessor(ParcelRecordProcessor processor);

  void registerAttachmentType(ParcelAttachmentType type);

  /** Declares an NBT world-position field rebased by the built-in declared-field processor. */
  void registerCoordinateField(ParcelCoordinateField field);

  /** Declares an NBT entity-UUID field rewritten when restore assigns fresh entity UUIDs. */
  void registerEntityRefField(ParcelEntityRefField field);

  /** Registers a capture contributor for world-external regional data. */
  void registerContributor(ParcelCaptureContributor contributor);

  /**
   * Declares an NBT field with no cross-snapshot semantics, eliminated on capture or rewritten as
   * a game-time offset (definition 2.5, rule 2.3).
   */
  void registerTransientField(
      io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField field);
}
