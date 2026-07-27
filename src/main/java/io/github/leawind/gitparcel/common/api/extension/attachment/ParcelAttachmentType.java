package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import net.minecraft.resources.Identifier;

/** Restores a registered kind of world-external parcel attachment. */
public interface ParcelAttachmentType {
  Identifier id();

  int schemaVersion();

  void restore(ParcelRecordProcessorContext context, AttachmentRecord attachment) throws Exception;
}
