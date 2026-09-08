package io.github.leawind.gitparcel.gametest.ext;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import net.minecraft.resources.Identifier;

/**
 * Attachment type for the region contributor's payload. The contributor consumes the raw records
 * itself, so the type's restore hook is a schema-version carrier only.
 */
public enum RegionDataAttachmentType implements ParcelAttachmentType {
  INSTANCE;

  @Override
  public Identifier id() {
    return RegionMarkerContributor.ATTACHMENT_TYPE;
  }

  @Override
  public int schemaVersion() {
    return 1;
  }

  @Override
  public void restore(ParcelRecordProcessorContext context, AttachmentRecord attachment) {}
}
