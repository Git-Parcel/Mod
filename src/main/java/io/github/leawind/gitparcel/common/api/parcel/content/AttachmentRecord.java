package io.github.leawind.gitparcel.common.api.parcel.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/** A resource required by, but stored outside, normal block and entity records. */
public record AttachmentRecord(
    LocalAttachmentId id,
    Identifier type,
    int schemaVersion,
    boolean required,
    CompoundTag payload) {
  public AttachmentRecord {
    if (schemaVersion < 0) {
      throw new IllegalArgumentException("schemaVersion must be non-negative");
    }
    payload = payload.copy();
  }
}
