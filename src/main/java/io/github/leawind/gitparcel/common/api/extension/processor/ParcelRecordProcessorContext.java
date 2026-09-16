package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentCollector;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import java.util.Objects;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Common context for semantic processing.
 *
 * @param collector the attachment collector, non-null only while capturing; restore passes {@code
 *     null} and processors must call {@link #requireCollector()} only from capture methods
 * @param semantics the snapshot's self-description while restoring, {@code null} while capturing;
 *     rule 7.3 participation is adjudicated against it
 */
public record ParcelRecordProcessorContext(
    LevelAccessor level,
    ParcelSpace space,
    ParcelAttachmentContext attachments,
    @Nullable ParcelAttachmentCollector collector,
    @Nullable ParcelSemantics semantics) {

  /** Returns the capture-time attachment collector. */
  public ParcelAttachmentCollector requireCollector() {
    return Objects.requireNonNull(
        collector, "Attachment collection is only available while capturing a parcel");
  }
}
