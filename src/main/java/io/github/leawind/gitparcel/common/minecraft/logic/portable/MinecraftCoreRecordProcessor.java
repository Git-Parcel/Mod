package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Normalizes vanilla spatial fields shared by all block entities and entities. */
public final class MinecraftCoreRecordProcessor implements ParcelRecordProcessor {
  public static final Identifier ID =
      Identifier.fromNamespaceAndPath("gitparcel", "minecraft_core");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntity source, BlockEntityRecord record) {
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
      ParcelRecordProcessorContext context, Entity source, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(data, context.space(), false);
    putVec3(data, "Pos", record.pos());
    if (data.contains("block_pos")) {
      putBlockPos(data, "block_pos", record.blockPos());
    }
    putVec3(data, "Motion", context.space().toParcelVector(source.getDeltaMovement()));
    putRotation(data, context.space().toParcelYaw(source.getYRot()), source.getXRot());
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
      net.minecraft.nbt.CompoundTag data, ParcelSpace space, boolean toWorld) {
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
  }

  private static void putBlockPos(net.minecraft.nbt.CompoundTag data, BlockPos pos) {
    data.putInt("x", pos.getX());
    data.putInt("y", pos.getY());
    data.putInt("z", pos.getZ());
  }

  private static void putBlockPos(
      net.minecraft.nbt.CompoundTag data, String key, BlockPos pos) {
    data.put(key, BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow());
  }

  private static void putVec3(net.minecraft.nbt.CompoundTag data, String key, Vec3 value) {
    ListTag list = new ListTag();
    list.add(DoubleTag.valueOf(value.x));
    list.add(DoubleTag.valueOf(value.y));
    list.add(DoubleTag.valueOf(value.z));
    data.put(key, list);
  }

  private static java.util.Optional<Vec3> readVec3(
      net.minecraft.nbt.CompoundTag data, String key) {
    return data.getList(key)
        .filter(list -> list.size() >= 3)
        .map(
            list ->
                new Vec3(
                    list.getDouble(0).orElse(0.0),
                    list.getDouble(1).orElse(0.0),
                    list.getDouble(2).orElse(0.0)));
  }

  private static void putRotation(
      net.minecraft.nbt.CompoundTag data, float yaw, float pitch) {
    ListTag list = new ListTag();
    list.add(FloatTag.valueOf(yaw));
    list.add(FloatTag.valueOf(pitch));
    data.put("Rotation", list);
  }

  private static java.util.Optional<Float> readYaw(net.minecraft.nbt.CompoundTag data) {
    return data.getList("Rotation")
        .filter(list -> !list.isEmpty())
        .map(list -> list.getFloat(0).orElse(0.0F));
  }

  private static float readPitch(net.minecraft.nbt.CompoundTag data) {
    return data.getList("Rotation")
        .filter(list -> list.size() >= 2)
        .map(list -> list.getFloat(1).orElse(0.0F))
        .orElse(0.0F);
  }
}
