package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/** Capture-time extension of the attachment context. */
public interface ParcelAttachmentCollector extends ParcelAttachmentContext {
  /**
   * Adds an attachment once per type and equality-based source identity, then returns its local ID.
   */
  LocalAttachmentId collect(
      Object sourceIdentity,
      Identifier type,
      int schemaVersion,
      boolean required,
      CompoundTag payload);
}
