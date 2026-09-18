package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentCollector;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelExtent;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Common neutral context for semantic processing.
 *
 * @param collector the attachment collector, non-null only while capturing; restore passes {@code
 *     null} and processors must call {@link #requireCollector()} only from capture methods
 * @param semantics the snapshot's self-description while restoring, {@code null} while capturing;
 *     rule 7.3 participation is adjudicated against it
 * @param extent the parcel's anchor-relative content extent, used for inside/outside detection
 *     (definition 3.2); {@code null} degrades spatial edges to always inside-pointing
 * @param gameTime the game time of the capture or restore operation; the anchor for rule 2.3 time
 *     relativization, constant for one operation
 */
public record ParcelRecordProcessorContext(
    ParcelSpace space,
    ParcelAttachmentContext attachments,
    @Nullable ParcelAttachmentCollector collector,
    @Nullable ParcelSemantics semantics,
    @Nullable ParcelExtent extent,
    long gameTime) {

  /** Returns the capture-time attachment collector. */
  public ParcelAttachmentCollector requireCollector() {
    return Objects.requireNonNull(
        collector, "Attachment collection is only available while capturing a parcel");
  }
}
