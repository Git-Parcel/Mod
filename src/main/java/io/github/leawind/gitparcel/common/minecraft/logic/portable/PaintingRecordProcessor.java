package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import java.util.ArrayList;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * Preserves a painting's wall attachment and facing in parcel space.
 *
 * <p>Vanilla serializes the wall direction as the {@code facing} field with the legacy 2D data
 * value encoding ({@link Direction#from2DDataValue}); capture re-derives the direction from it
 * instead of the live entity.
 */
public final class PaintingRecordProcessor implements ParcelRecordProcessor {
  public static final Identifier ID = Identifier.fromNamespaceAndPath("gitparcel", "painting");

  private static final Identifier PAINTING_TYPE =
      Identifier.fromNamespaceAndPath("minecraft", "painting");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public java.util.Set<Identifier> runAfter() {
    return java.util.Set.of(MinecraftCoreRecordProcessor.ID);
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    if (!PAINTING_TYPE.equals(record.type())) {
      return record;
    }
    // The 26.x facing key stores a legacy-id byte, so numeric reads must not assume an int tag.
    if (!(record.data().get("facing") instanceof net.minecraft.nbt.NumericTag number)) {
      return record;
    }
    int facing = NbtReads.intValue(number);
    if (facing < 0) {
      return record;
    }
    CompoundTag payload = new CompoundTag();
    payload.putString(
        "direction",
        context.space().toParcelDirection(Direction.from2DDataValue(facing)).getName());
    var semantic = new ArrayList<>(record.semanticData());
    semantic.add(new SemanticData(ID, 0, payload));
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), record.data(), semantic);
  }

  @Override
  public EntityRecord restoreEntity(ParcelRecordProcessorContext context, EntityRecord record)
      throws ParcelException {
    for (SemanticData semantic : record.semanticData()) {
      if (!semantic.processor().equals(ID)) {
        continue;
      }
      if (semantic.schemaVersion() != 0) {
        throw new ParcelException(
            "Unsupported painting semantic schema version: " + semantic.schemaVersion());
      }
      String name = NbtReads.getString(semantic.payload(), "direction", "south");
      Direction local = Direction.byName(name);
      if (local == null) {
        local = Direction.SOUTH;
      }
      Direction world = context.space().toWorldDirection(local);
      var data = record.data().copy();
      data.put("facing", net.minecraft.nbt.ByteTag.valueOf((byte) world.get2DDataValue()));
      return new EntityRecord(
          record.type(), record.pos(), record.blockPos(), data, record.semanticData());
    }
    return record;
  }
}
