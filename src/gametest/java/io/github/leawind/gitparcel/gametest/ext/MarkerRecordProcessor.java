package io.github.leawind.gitparcel.gametest.ext;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Attaches a marker payload to every captured entity and re-materializes it as the entity's custom
 * name on restore, proving that processors can channel world-external data through attachments.
 */
public enum MarkerRecordProcessor implements ParcelRecordProcessor {
  INSTANCE;

  static final Identifier ATTACHMENT_TYPE =
      Identifier.fromNamespaceAndPath("gitparceltest", "marker");
  public static final String MARKER_VALUE = "carried-by-attachment";
  private static final String REF_TAG = "gitparceltest:marker_ref";

  @Override
  public Identifier id() {
    return Identifier.fromNamespaceAndPath("gitparceltest", "marker_processor");
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, Entity source, EntityRecord record) {
    var payload = new CompoundTag();
    payload.putString(MarkerAttachmentType.PAYLOAD_KEY, MARKER_VALUE);
    LocalAttachmentId ref =
        context
            .requireCollector()
            .collect("marker", ATTACHMENT_TYPE, MarkerAttachmentType.INSTANCE.schemaVersion(), true, payload);
    var data = record.data().copy();
    data.putString(REF_TAG, ref.value());
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, List.of());
  }

  @Override
  public EntityRecord restoreEntity(ParcelRecordProcessorContext context, EntityRecord record)
      throws ParcelException {
    String ref = record.data().getString(REF_TAG).orElse("");
    if (ref.isEmpty()) {
      return record;
    }
    String resolved =
        context
            .attachments()
            .findResolved(new LocalAttachmentId(ref), String.class)
            .orElseThrow(
                () ->
                    new ParcelException(
                        "Marker attachment was not resolved before entity restore: " + ref));
    var data = record.data().copy();
    data.remove(REF_TAG);
    data.putString("CustomName", resolved);
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, List.of());
  }
}
