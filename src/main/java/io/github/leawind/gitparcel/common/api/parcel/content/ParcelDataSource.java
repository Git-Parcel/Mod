package io.github.leawind.gitparcel.common.api.parcel.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;

/**
 * A one-shot, bounded-memory source of portable parcel content.
 *
 * <p>Attachments are enumerated after blocks and entities because processors may discover them
 * while those records are captured.
 */
public interface ParcelDataSource {
  void forEachBlockSection(int sectionSize, ParcelDataConsumer<BlockSection> consumer)
      throws IOException, ParcelException;

  void forEachEntity(ParcelDataConsumer<EntityRecord> consumer)
      throws IOException, ParcelException;

  void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer)
      throws IOException, ParcelException;
}
