package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import net.minecraft.resources.Identifier;

/** Restores a registered kind of world-external parcel attachment. */
public interface ParcelAttachmentType {
  Identifier id();

  int schemaVersion();

  /**
   * Explicit tie-break priority for rule 7.4 adjudication among same-tier duplicate
   * registrations; higher wins.
   */
  default int registrationPriority() {
    return 0;
  }

  void restore(ParcelAttachmentRestoreContext context, AttachmentRecord attachment)
      throws Exception;
}
