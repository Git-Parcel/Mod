package io.github.leawind.gitparcel.common.api.extension;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
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
}
