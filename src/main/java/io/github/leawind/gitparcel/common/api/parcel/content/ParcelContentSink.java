package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;

/** Receives portable records in attachment, block-section, then entity order. */
public interface ParcelContentSink {
  default void acceptAttachment(AttachmentRecord attachment) throws IOException, ParcelException {}

  default void acceptBlockSection(BlockSection section) throws IOException, ParcelException {}

  default void acceptEntity(EntityRecord entity) throws IOException, ParcelException {}

  default void finish() throws IOException, ParcelException {}
}
