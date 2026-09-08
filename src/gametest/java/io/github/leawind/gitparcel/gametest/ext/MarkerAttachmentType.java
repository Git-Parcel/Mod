package io.github.leawind.gitparcel.gametest.ext;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import net.minecraft.resources.Identifier;

/**
 * Resolves the restored marker attachment to its payload string so {@link MarkerRecordProcessor}
 * can read it while restoring entities.
 */
public enum MarkerAttachmentType implements ParcelAttachmentType {
  INSTANCE;

  static final Identifier ID = MarkerRecordProcessor.ATTACHMENT_TYPE;
  static final String PAYLOAD_KEY = "value";

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public int schemaVersion() {
    return 1;
  }

  @Override
  public void restore(ParcelRecordProcessorContext context, AttachmentRecord attachment)
      throws Exception {
    context
        .attachments()
        .resolve(
            attachment.id(),
            attachment.payload().getString(PAYLOAD_KEY).orElseThrow());
  }
}
