package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import java.util.ArrayList;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;

/** Preserves a painting's wall attachment and facing in parcel space. */
public final class PaintingRecordProcessor implements ParcelRecordProcessor {
  public static final Identifier ID = Identifier.fromNamespaceAndPath("gitparcel", "painting");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public Set<Identifier> runAfter() {
    return Set.of(MinecraftCoreRecordProcessor.ID);
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, Entity source, EntityRecord record) {
    if (!(source instanceof Painting painting)) {
      return record;
    }
    CompoundTag payload = new CompoundTag();
    payload.putString(
        "direction", context.space().toParcelDirection(painting.getDirection()).getName());
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
      String name = semantic.payload().getString("direction").orElse("south");
      Direction local = Direction.byName(name);
      if (local == null) {
        local = Direction.SOUTH;
      }
      Direction world = context.space().toWorldDirection(local);
      var data = record.data().copy();
      data.putInt("facing", world.get2DDataValue());
      return new EntityRecord(
          record.type(), record.pos(), record.blockPos(), data, record.semanticData());
    }
    return record;
  }
}
