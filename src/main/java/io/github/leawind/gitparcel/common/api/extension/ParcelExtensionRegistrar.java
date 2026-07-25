package io.github.leawind.gitparcel.common.api.extension;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessor;

/** Registration surface exposed to a {@link GitParcelExtension}. */
public interface ParcelExtensionRegistrar {
  /**
   * Registers a parcel format implementation.
   *
   * <p>Registrations from one extension are committed atomically after the extension returns.
   */
  void registerFormat(ParcelFormat.Impl<?> format);

  void registerProcessor(ParcelDataProcessor processor);

  void registerAttachmentType(ParcelAttachmentType type);
}
