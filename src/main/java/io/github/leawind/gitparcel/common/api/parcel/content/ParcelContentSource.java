package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;

/**
 * A one-shot, bounded-memory source of portable parcel content.
 *
 * <p>Attachments are enumerated after blocks and entities because processors may discover them
 * while those records are captured.
 */
public interface ParcelContentSource {
  void forEachBlockSection(int sectionSize, ParcelContentConsumer<BlockSection> consumer)
      throws IOException, ParcelException;

  void forEachEntity(ParcelContentConsumer<EntityRecord> consumer)
      throws IOException, ParcelException;

  void forEachAttachment(ParcelContentConsumer<AttachmentRecord> consumer)
      throws IOException, ParcelException;
}
