package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import net.minecraft.server.level.ServerLevel;

/**
 * Live-world context handed to {@link ParcelAttachmentType#restore}.
 *
 * <p>Materializing an attachment into the target world is one of the few explicitly marked version
 * seams (DESIGN.md, "接口稳定性与兼容共存"): attachment types receive the restore level directly,
 * while record processors stay neutral.
 */
public interface ParcelAttachmentRestoreContext {
  /** The target server level; a marked version seam. */
  ServerLevel level();

  /**
   * Associates a target-world runtime value with the portable attachment ID.
   *
   * <p>Attachment types call this while restoring; data processors resolve it afterwards through
   * their neutral context.
   */
  void resolve(LocalAttachmentId id, Object value);
}
