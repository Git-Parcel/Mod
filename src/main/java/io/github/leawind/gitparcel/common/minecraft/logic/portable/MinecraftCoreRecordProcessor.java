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
/*? if <26.1 {*/
/*import net.minecraft.nbt.Tag;
 *//*?}*/
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
    /*? if >=26.1 {*/
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
    data.read("block_pos", BlockPos.CODEC)
        .map(value -> toWorld ? space.toWorld(value) : space.toParcel(value))
        .ifPresent(value -> putBlockPos(data, "block_pos", value));
    if (toWorld) {
      data.remove("UUID");
    }
    data.getList("Passengers")
        .ifPresent(
            passengers ->
                passengers
                    .compoundStream()
                    .forEach(passenger -> transformEntityTree(passenger, space, toWorld)));
    /*?} else {*/
    /*readVec3(data, "Pos")
        .ifPresent(
            value -> putVec3(data, "Pos", toWorld ? space.toWorld(value) : space.toParcel(value)));
    readVec3(data, "Motion")
        .ifPresent(
            value ->
                putVec3(
                    data,
                    "Motion",
                    toWorld ? space.toWorldVector(value) : space.toParcelVector(value)));
    java.util.Optional<Float> yaw = readYaw(data);
    if (yaw.isPresent()) {
      putRotation(
          data,
          toWorld ? space.toWorldYaw(yaw.get()) : space.toParcelYaw(yaw.get()),
          readPitch(data));
    }
    BlockPos.CODEC
        .parse(NbtOps.INSTANCE, data.get("block_pos"))
        .result()
        .map(value -> toWorld ? space.toWorld(value) : space.toParcel(value))
        .ifPresent(value -> putBlockPos(data, "block_pos", value));
    if (toWorld) {
      data.remove("UUID");
    }
    var passengers = data.getList("Passengers", Tag.TAG_COMPOUND);
    for (int i = 0; i < passengers.size(); i++) {
      transformEntityTree(passengers.getCompound(i), space, toWorld);
    }
    *//*?}*/
  }

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
    /*? if >=26.1 {*/
    return data.getList(key)
        .filter(list -> list.size() >= 3)
        .map(
            list ->
                new Vec3(
                    list.getDouble(0).orElse(0.0),
                    list.getDouble(1).orElse(0.0),
                    list.getDouble(2).orElse(0.0)));
    /*?} else {*/
    /*var list = data.getList(key, Tag.TAG_DOUBLE);
    if (list.size() < 3) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(
        new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2)));
    *//*?}*/
  }

  private static void putRotation(CompoundTag data, float yaw, float pitch) {
    ListTag list = new ListTag();
    list.add(FloatTag.valueOf(yaw));
    list.add(FloatTag.valueOf(pitch));
    data.put("Rotation", list);
  }

  private static java.util.Optional<Float> readYaw(CompoundTag data) {
    /*? if >=26.1 {*/
    return data.getList("Rotation")
        .filter(list -> !list.isEmpty())
        .map(list -> list.getFloat(0).orElse(0.0F));
    /*?} else {*/
    /*var list = data.getList("Rotation", Tag.TAG_FLOAT);
    if (list.isEmpty()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(list.getFloat(0));
    *//*?}*/
  }

  private static float readPitch(CompoundTag data) {
    /*? if >=26.1 {*/
    return data.getList("Rotation")
        .filter(list -> list.size() >= 2)
        .map(list -> list.getFloat(1).orElse(0.0F))
        .orElse(0.0F);
    /*?} else {*/
    /*var list = data.getList("Rotation", Tag.TAG_FLOAT);
    return list.size() >= 2 ? list.getFloat(1) : 0.0F;
    *//*?}*/
  }
}
