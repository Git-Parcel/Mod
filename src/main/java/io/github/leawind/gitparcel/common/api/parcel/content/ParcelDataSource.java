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
  /**
   * Emits the parcel's blocks in cubic sections of the requested edge length.
   *
   * <p>Section origins are expressed relative to the parcel anchor (they may be negative), matching
   * the anchor-relative coordinates {@link ParcelDataSink} sections use on load. The section
   * boundaries follow {@code BlockSectionPartitioner.partition(parcelSize, anchor, sectionSize)}
   * shifted by the anchor, so a save and a load of the same parcel agree on section file indices.
   */
  void forEachBlockSection(int sectionSize, ParcelDataConsumer<BlockSection> consumer)
      throws IOException, ParcelException;

  void forEachEntity(ParcelDataConsumer<EntityRecord> consumer)
      throws IOException, ParcelException;

  void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer)
      throws IOException, ParcelException;
}
