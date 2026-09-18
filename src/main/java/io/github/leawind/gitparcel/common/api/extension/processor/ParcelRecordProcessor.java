package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Transforms Minecraft data into and out of portable parcel records.
 *
 * <p>Implementations must be stateless. Methods are invoked on the server thread and only ever see
 * the neutral record and the neutral {@link ParcelRecordProcessorContext}; live game objects never
 * cross this boundary. World lookups needed for attachment collection go through {@code
 * ParcelAttachmentCollector#level()}, an explicitly marked version seam.
 */
public interface ParcelRecordProcessor {
  Identifier id();

  default Set<Identifier> runAfter() {
    return Set.of();
  }

  /**
   * Explicit tie-break priority for rule 7.4 adjudication among same-tier duplicate
   * registrations; higher wins.
   */
  default int registrationPriority() {
    return 0;
  }

  default Set<Identifier> runBefore() {
    return Set.of();
  }

  default BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    return record;
  }

  default BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    return record;
  }

  default EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    return record;
  }

  default EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    return record;
  }
}
