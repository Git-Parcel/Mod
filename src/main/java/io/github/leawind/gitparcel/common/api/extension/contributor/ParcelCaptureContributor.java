package io.github.leawind.gitparcel.common.api.extension.contributor;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import net.minecraft.resources.Identifier;

/**
 * Captures and restores world-external data tied to a parcel region that is not reachable from any
 * entity or block-entity record (for example a mod's per-region saved data).
 *
 * <p>Data reachable from records should use record processors with attachments instead. Methods
 * run on the server thread; capture happens before the attachment set drains into the snapshot,
 * restore happens after every block, entity and deferred commit was applied.
 */
public interface ParcelCaptureContributor {
  Identifier id();

  /** Collects regional data through {@link ParcelCaptureContext#collector()}. */
  void capture(ParcelCaptureContext context) throws Exception;

  /**
   * Re-applies regional data. The context lists every attachment record restored in this apply;
   * filter by the attachment types this contributor collects with.
   */
  void restore(ParcelRestoreContext context) throws Exception;
}
