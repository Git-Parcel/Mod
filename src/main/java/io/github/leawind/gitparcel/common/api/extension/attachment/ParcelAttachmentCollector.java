package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * Capture-time extension of the attachment context.
 *
 * <p>The collector is one of the few explicitly marked version seams: attachment collection may
 * need live world state (for example looking up map data), so it carries the capturing level. See
 * DESIGN.md, "接口稳定性与兼容共存".
 */
public interface ParcelAttachmentCollector extends ParcelAttachmentContext {
  /** The live capturing level; a marked version seam, available only during capture. */
  Level level();

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
