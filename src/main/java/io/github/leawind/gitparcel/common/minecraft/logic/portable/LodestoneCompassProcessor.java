package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Rebases the lodestone position of compass items embedded in records (SEMANTICS.md section 9
 * resource-edge audit). The position is a spatial edge judged geometrically: a lodestone inside
 * the parcel travels with it, one outside the parcel keeps its world value. The dimension
 * identifier travels unchanged; a cross-dimension lodestone whose coordinates happen to fall
 * inside the extent is accepted per rule 3.2's ambiguity note.
 *
 * <p>Vanilla forms: 26.x stores the tracker as the {@code minecraft:lodestone_tracker}
 * component with {@code target.pos} in {@link BlockPos#CODEC} form; 1.20.1 stores
 * {@code tag.LodestonePos} as an {@code X/Y/Z} compound.
 */
public final class LodestoneCompassProcessor implements ParcelRecordProcessor {
  public static final Identifier ID =
      Identifier.fromNamespaceAndPath("gitparcel", "lodestone_compass");

  private static final String COMPASS = "minecraft:compass";
  /*? if >=26.1 {*/
  private static final String COMPONENTS = "components";
  private static final String TRACKER_COMPONENT = "minecraft:lodestone_tracker";
  private static final String TARGET_KEY = "target";
  private static final String POS_KEY = "pos";
  /*?} else {*/
  /*private static final String TAG = "tag";
  private static final String LODESTONE_POS = "LodestonePos";
  *//*?}*/

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachBlockEntityItem(data, item -> transformItem(item, context, false));
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachBlockEntityItem(data, item -> transformItem(item, context, true));
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachEntityItem(data, item -> transformItem(item, context, false));
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) throws ParcelException {
    var data = record.data().copy();
    ItemStackNbtWalker.forEachEntityItem(data, item -> transformItem(item, context, true));
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private static void transformItem(
      CompoundTag item, ParcelRecordProcessorContext context, boolean toWorld)
      throws ParcelException {
    if (!COMPASS.equals(NbtReads.getString(item, "id", ""))) {
      return;
    }
    /*? if >=26.1 {*/
    var components = NbtReads.getCompound(item, COMPONENTS);
    if (components == null) {
      return;
    }
    var tracker = NbtReads.getCompound(components, TRACKER_COMPONENT);
    if (tracker == null) {
      return;
    }
    var target = NbtReads.getCompound(tracker, TARGET_KEY);
    if (target == null) {
      return;
    }
    NbtReads.read(target, POS_KEY, BlockPos.CODEC)
        .ifPresent(pos -> rebasePosition(target, POS_KEY, pos, context, toWorld));
    /*?} else {*/
    /*var tag = NbtReads.getCompound(item, TAG);
    if (tag == null) {
      return;
    }
    var stored = NbtReads.getCompound(tag, LODESTONE_POS);
    if (stored == null) {
      return;
    }
    Integer x = NbtReads.getIntOrNull(stored, "X");
    Integer y = NbtReads.getIntOrNull(stored, "Y");
    Integer z = NbtReads.getIntOrNull(stored, "Z");
    if (x == null || y == null || z == null) {
      return;
    }
    rebasePosition(tag, LODESTONE_POS, new BlockPos(x, y, z), context, toWorld);
    *//*?}*/
  }

  /**
   * Rebases one lodestone position and writes it back in the era's own form: the codec-encoded
   * int array on 26.x, the legacy {@code X/Y/Z} compound on 1.20.1.
   */
  /*? if >=26.1 {*/
  private static void rebasePosition(
      CompoundTag container,
      String key,
      BlockPos pos,
      ParcelRecordProcessorContext context,
      boolean toWorld) {
    var space = context.space();
    if (!SpatialEdges.shouldRebase(
        context, space, new Vec3(pos.getX(), pos.getY(), pos.getZ()), toWorld)) {
      return;
    }
    BlockPos rebased = SpatialEdges.rebase(space, pos, toWorld);
    container.put(key, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, rebased).result().orElseThrow());
  }
  /*?} else {*/
  /*private static void rebasePosition(
      CompoundTag container,
      String key,
      BlockPos pos,
      ParcelRecordProcessorContext context,
      boolean toWorld) {
    var space = context.space();
    if (!SpatialEdges.shouldRebase(
        context, space, new Vec3(pos.getX(), pos.getY(), pos.getZ()), toWorld)) {
      return;
    }
    BlockPos rebased = SpatialEdges.rebase(space, pos, toWorld);
    var stored = new CompoundTag();
    stored.putInt("X", rebased.getX());
    stored.putInt("Y", rebased.getY());
    stored.putInt("Z", rebased.getZ());
    container.put(key, stored);
  }
  *//*?}*/
}
