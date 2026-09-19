package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentRestoreContext;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentType;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
/*? if >=26.1 {*/
import net.minecraft.world.level.saveddata.maps.MapId;
/*?}*/
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * Carries vanilla map art ({@code MapItemSavedData}) inside the snapshot and re-materializes it
 * under a fresh map id in the target level.
 *
 * <p>Map decorations keep their original world coordinates: a map is a picture of where it was
 * drawn and must not be rebased with the parcel.
 */
public enum MapDataAttachmentType implements ParcelAttachmentType {
  INSTANCE;

  public static final Identifier ID = Identifier.fromNamespaceAndPath("gitparcel", "map_data");
  public static final int SCHEMA_VERSION = 1;

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public int schemaVersion() {
    return SCHEMA_VERSION;
  }

  @Override
  public void restore(ParcelAttachmentRestoreContext context, AttachmentRecord attachment)
      throws Exception {
    /*? if >=26.1 {*/
    MapItemSavedData data =
        MapItemSavedData.CODEC
            .parse(NbtOps.INSTANCE, attachment.payload())
            .result()
            .orElseThrow(
                () ->
                    new ParcelException(
                        "Corrupt map payload for attachment " + attachment.id()));
    var level = context.level();
    MapId newId = level.getServer().overworld().getFreeMapId();
    level.setMapData(newId, data);
    /*?} else {*/
    /*MapItemSavedData data = MapItemSavedData.load(attachment.payload());
    if (data == null) {
      throw new ParcelException("Corrupt map payload for attachment " + attachment.id());
    }
    var level = context.level();
    int newId = level.getServer().overworld().getFreeMapId();
    level.setMapData("map_" + newId, data);
    *//*?}*/
    context.resolve(attachment.id(), newId);
  }

  /** Serializes map data for the attachment payload. */
  public static CompoundTag payloadOf(MapItemSavedData data) {
    /*? if >=26.1 {*/
    return (CompoundTag)
        MapItemSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
    /*?} else {*/
    /*return data.save(new CompoundTag());
    *//*?}*/
  }
}
