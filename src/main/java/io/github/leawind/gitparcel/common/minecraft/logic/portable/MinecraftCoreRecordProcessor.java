package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Normalizes vanilla spatial fields shared by all block entities and entities.
 *
 * <p>The entity tree (Pos, Motion, Rotation, block_pos) is transformed straight from the NBT that
 * vanilla serialization produced; every value the transform needs is already in the record.
 */
public final class MinecraftCoreRecordProcessor implements ParcelRecordProcessor {
  public static final Identifier ID =
      Identifier.fromNamespaceAndPath("gitparcel", "minecraft_core");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var data = record.data().copy();
    putBlockPos(data, record.pos());
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var data = record.data().copy();
    putBlockPos(data, context.space().toWorld(record.pos()));
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(data, context.space(), false);
    putVec3(data, "Pos", record.pos());
    if (data.contains("block_pos")) {
      putBlockPos(data, "block_pos", record.blockPos());
    }
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(data, context.space(), true);
    putVec3(data, "Pos", context.space().toWorld(record.pos()));
    if (data.contains("block_pos")) {
      putBlockPos(data, "block_pos", context.space().toWorld(record.blockPos()));
    }
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private static void transformEntityTree(
      CompoundTag data, ParcelSpace space, boolean toWorld) {
    readVec3(data, "Pos")
        .map(value -> toWorld ? space.toWorld(value) : space.toParcel(value))
        .ifPresent(value -> putVec3(data, "Pos", value));
    readVec3(data, "Motion")
        .map(value -> toWorld ? space.toWorldVector(value) : space.toParcelVector(value))
        .ifPresent(value -> putVec3(data, "Motion", value));
    readYaw(data)
        .ifPresent(
            yaw ->
                putRotation(
                    data,
                    toWorld ? space.toWorldYaw(yaw) : space.toParcelYaw(yaw),
                    readPitch(data)));
    NbtReads.read(data, "block_pos", BlockPos.CODEC)
        .map(value -> toWorld ? space.toWorld(value) : space.toParcel(value))
        .ifPresent(value -> putBlockPos(data, "block_pos", value));
    /*? if <26.1 {*/
    /*transformTilePos(data, space, toWorld);
    *//*?}*/
    if (toWorld) {
      data.remove("UUID");
    }
    EntityTrees.forEachPassenger(data, passenger -> transformEntityTree(passenger, space, toWorld));
  }

  /**
   * 1.20.1 keeps the authoritative position of hanging entities (paintings, item frames, leash
   * knots) in the flat {@code TileX/TileY/TileZ} keys; 26.x moved it to {@code block_pos}.
   */
  /*? if <26.1 {*/
  /*private static void transformTilePos(CompoundTag data, ParcelSpace space, boolean toWorld) {
    Integer x = NbtReads.getIntOrNull(data, "TileX");
    Integer y = NbtReads.getIntOrNull(data, "TileY");
    Integer z = NbtReads.getIntOrNull(data, "TileZ");
    if (x == null || y == null || z == null) {
      return;
    }
    BlockPos rebased =
        toWorld ? space.toWorld(new BlockPos(x, y, z)) : space.toParcel(new BlockPos(x, y, z));
    data.putInt("TileX", rebased.getX());
    data.putInt("TileY", rebased.getY());
    data.putInt("TileZ", rebased.getZ());
  }
  *//*?}*/

  private static void putBlockPos(CompoundTag data, BlockPos pos) {
    data.putInt("x", pos.getX());
    data.putInt("y", pos.getY());
    data.putInt("z", pos.getZ());
  }

  private static void putBlockPos(CompoundTag data, String key, BlockPos pos) {
    data.put(key, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().orElseThrow());
  }

  private static void putVec3(CompoundTag data, String key, Vec3 value) {
    ListTag list = new ListTag();
    list.add(DoubleTag.valueOf(value.x));
    list.add(DoubleTag.valueOf(value.y));
    list.add(DoubleTag.valueOf(value.z));
    data.put(key, list);
  }

  private static java.util.Optional<Vec3> readVec3(CompoundTag data, String key) {
    var list = NbtReads.getList(data, key);
    if (list == null || list.size() < 3) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(
        new Vec3(
            NbtReads.getDouble(list, 0, 0.0),
            NbtReads.getDouble(list, 1, 0.0),
            NbtReads.getDouble(list, 2, 0.0)));
  }

  private static void putRotation(CompoundTag data, float yaw, float pitch) {
    ListTag list = new ListTag();
    list.add(FloatTag.valueOf(yaw));
    list.add(FloatTag.valueOf(pitch));
    data.put("Rotation", list);
  }

  private static java.util.Optional<Float> readYaw(CompoundTag data) {
    var list = NbtReads.getList(data, "Rotation");
    if (list == null || list.isEmpty()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(NbtReads.getFloat(list, 0, 0.0F));
  }

  private static float readPitch(CompoundTag data) {
    var list = NbtReads.getList(data, "Rotation");
    return list != null && list.size() >= 2 ? NbtReads.getFloat(list, 1, 0.0F) : 0.0F;
  }
}
