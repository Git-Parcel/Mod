package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;

/** Receives portable records in attachment, block-section, then entity order. */
public interface ParcelDataSink {
  default void acceptAttachment(AttachmentRecord attachment) throws IOException, ParcelException {}

  default void acceptBlockSection(BlockSection section) throws IOException, ParcelException {}

  default void acceptEntity(EntityRecord entity) throws IOException, ParcelException {}

  /**
   * Invoked once after every content type was delivered successfully. Sinks that defer work (for
   * example to remap entity references across the whole batch) commit here; a failed load never
   * reaches this point.
   */
  default void commit() throws IOException, ParcelException {}

  default void finish() throws IOException, ParcelException {}
}
