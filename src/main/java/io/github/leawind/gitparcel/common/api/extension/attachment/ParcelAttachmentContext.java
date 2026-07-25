package io.github.leawind.gitparcel.common.api.extension.attachment;

import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import java.util.Optional;

/** Operation-scoped attachment identities and restored runtime values. */
public interface ParcelAttachmentContext {
  /**
   * Associates a target-world runtime value with a portable attachment ID.
   *
   * <p>Attachment types call this during restore; data processors resolve it afterwards.
   */
  void resolve(LocalAttachmentId id, Object value);

  <T> Optional<T> findResolved(LocalAttachmentId id, Class<T> type);
}
