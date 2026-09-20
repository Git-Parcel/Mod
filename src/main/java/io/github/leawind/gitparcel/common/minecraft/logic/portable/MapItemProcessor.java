package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.MapItem;
/*? if >=26.1 {*/
import net.minecraft.world.level.saveddata.maps.MapId;
/*?}*/
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Makes filled maps travel with their artwork: on capture the referenced map data is collected as
 * an attachment and the item stores the attachment reference inside its custom-data component; on
 * restore the attachment is re-materialized under a fresh map id and the item is repointed.
 *
 * <p>The original {@code map_id} component is preserved through capture, so a snapshot read by an
 * older version without this processor degrades to today's behavior instead of corrupting the
 * item.
 */
public final class MapItemProcessor implements ParcelRecordProcessor {
  public static final Identifier ID = Identifier.fromNamespaceAndPath("gitparcel", "map_items");

  private static final String FILLED_MAP = "minecraft:filled_map";
  /*? if >=26.1 {*/
  private static final String COMPONENTS = "components";
  private static final String MAP_ID_COMPONENT = "minecraft:map_id";
  private static final String CUSTOM_DATA_COMPONENT = "minecraft:custom_data";
  private static final String REF_KEY = "gitparcel:map_attachment";
  /*?} else {*/
  /*private static final String TAG = "tag";
  private static final String LEGACY_MAP_KEY = "map";
  private static final String REF_KEY = "gitparcel:map_attachment";
  *//*?}*/

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachBlockEntityItem(data, item -> captureItem(item, context));
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachBlockEntityItem(data, item -> restoreItem(item, context));
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachEntityItem(data, item -> captureItem(item, context));
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachEntityItem(data, item -> restoreItem(item, context));
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private static void captureItem(CompoundTag item, ParcelRecordProcessorContext context) {
    if (!isFilledMap(item)) {
      return;
    }
    /*? if >=26.1 {*/
    var components = item.getCompound(COMPONENTS).orElse(null);
    if (components == null) {
      return;
    }
    var mapId = components.getInt(MAP_ID_COMPONENT).orElse(-1);
    if (mapId < 0) {
      return;
    }
    MapItemSavedData data =
        MapItem.getSavedData(new MapId(mapId), context.requireCollector().level());
    if (data == null) {
      return;
    }
    LocalAttachmentId ref =
        context
            .requireCollector()
            .collect(
                mapId,
                MapDataAttachmentType.ID,
                MapDataAttachmentType.SCHEMA_VERSION,
                true,
                MapDataAttachmentType.payloadOf(data));
    customDataOf(components).putString(REF_KEY, ref.value());
    /*?} else {*/
    /*var tag = item.contains(TAG) ? item.getCompound(TAG) : null;
    if (tag == null) {
      return;
    }
    // Id 0 is what a fresh world hands out for its first map, so presence has to be tested
    // explicitly: a non-positive test would silently drop that map's artwork.
    Integer mapId = NbtReads.getIntOrNull(tag, LEGACY_MAP_KEY);
    if (mapId == null) {
      return;
    }
    MapItemSavedData data = MapItem.getSavedData(mapId, context.requireCollector().level());
    if (data == null) {
      return;
    }
    LocalAttachmentId ref =
        context
            .requireCollector()
            .collect(
                mapId,
                MapDataAttachmentType.ID,
                MapDataAttachmentType.SCHEMA_VERSION,
                true,
                MapDataAttachmentType.payloadOf(data));
    tag.putString(REF_KEY, ref.value());
    *//*?}*/
  }

  private static void restoreItem(CompoundTag item, ParcelRecordProcessorContext context)
      throws ParcelException {
    /*? if >=26.1 {*/
    var components = item.getCompound(COMPONENTS).orElse(null);
    if (components == null) {
      return;
    }
    var customData = components.getCompound(CUSTOM_DATA_COMPONENT).orElse(null);
    if (customData == null) {
      return;
    }
    String ref = customData.getString(REF_KEY).orElse("");
    if (ref.isEmpty()) {
      return;
    }
    MapId newId =
        context
            .attachments()
            .findResolved(new LocalAttachmentId(ref), MapId.class)
            .orElseThrow(
                () ->
                    new ParcelException(
                        "Map attachment was not restored before its item: " + ref));
    var mapComponents = item.getCompound(COMPONENTS).orElseThrow();
    mapComponents.putInt(MAP_ID_COMPONENT, newId.id());
    var mapCustomData = mapComponents.getCompound(CUSTOM_DATA_COMPONENT).orElseThrow();
    mapCustomData.remove(REF_KEY);
    if (mapCustomData.isEmpty()) {
      mapComponents.remove(CUSTOM_DATA_COMPONENT);
    }
    /*?} else {*/
    /*var tag = item.contains(TAG) ? item.getCompound(TAG) : null;
    if (tag == null) {
      return;
    }
    String ref = tag.getString(REF_KEY);
    if (ref.isEmpty()) {
      return;
    }
    Integer newId =
        context
            .attachments()
            .findResolved(new LocalAttachmentId(ref), Integer.class)
            .orElseThrow(
                () ->
                    new ParcelException(
                        "Map attachment was not restored before its item: " + ref));
    var mapTag = item.getCompound(TAG);
    mapTag.putInt(LEGACY_MAP_KEY, newId);
    mapTag.remove(REF_KEY);
    *//*?}*/
  }

  private static boolean isFilledMap(CompoundTag item) {
    return FILLED_MAP.equals(NbtReads.getString(item, "id", ""));
  }

  /*? if >=26.1 {*/
  private static CompoundTag customDataOf(CompoundTag components) {
    var customData = components.getCompound(CUSTOM_DATA_COMPONENT).orElse(null);
    if (customData != null) {
      return customData;
    }
    customData = new CompoundTag();
    components.put(CUSTOM_DATA_COMPONENT, customData);
    return customData;
  }
  /*?}*/
}
