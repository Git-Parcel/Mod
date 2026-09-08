package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Rebases NBT fields declared through {@link ParcelCoordinateFieldRegistry} between world space
 * and parcel space.
 *
 * <p>Runs after the core processor so root spatial fields are already normalized. Entity fields
 * also apply inside the {@code Passengers} subtree, filtered by each nested entity's own type.
 */
public final class DeclaredCoordinateFieldProcessor implements ParcelRecordProcessor {
  public static final Identifier ID =
      Identifier.fromNamespaceAndPath("gitparcel", "declared_fields");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public Set<Identifier> runAfter() {
    return Set.of(MinecraftCoreRecordProcessor.ID);
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntity source, BlockEntityRecord record) {
    var typeId = record.data().getString("id").map(Identifier::parse).orElse(null);
    var data = record.data().copy();
    transformBlockEntity(data, typeId, context.space(), false);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var typeId = record.data().getString("id").map(Identifier::parse).orElse(null);
    var data = record.data().copy();
    transformBlockEntity(data, typeId, context.space(), true);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, Entity source, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(data, record.type(), context.space(), false);
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(data, record.type(), context.space(), true);
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private static void transformBlockEntity(
      CompoundTag data, @Nullable Identifier typeId, ParcelSpace space, boolean toWorld) {
    if (typeId == null) {
      return;
    }
    var fields = applicableFields(ParcelCoordinateField.Target.BLOCK_ENTITY, typeId);
    if (fields.isEmpty()) {
      return;
    }
    for (var field : fields) {
      var segments = NbtPaths.parse(field.path());
      NbtPaths.forEach(
          data,
          segments,
          slot -> rebaseSlot(slot, field.encoding(), space, toWorld));
    }
  }

  private static void transformEntityTree(
      CompoundTag data, Identifier typeId, ParcelSpace space, boolean toWorld) {
    var fields = applicableFields(ParcelCoordinateField.Target.ENTITY, typeId);
    for (var field : fields) {
      NbtPaths.forEach(
          data, NbtPaths.parse(field.path()), slot -> rebaseSlot(slot, field.encoding(), space, toWorld));
    }
    data.getList("Passengers")
        .ifPresent(
            passengers ->
                passengers
                    .compoundStream()
                    .forEach(
                        passenger ->
                            transformEntityTree(
                                passenger,
                                passenger.getString("id").map(Identifier::parse).orElse(null),
                                space,
                                toWorld)));
  }

  private static List<ParcelCoordinateField> applicableFields(
      ParcelCoordinateField.Target target, @Nullable Identifier typeId) {
    return ParcelCoordinateFieldRegistry.get().fields().stream()
        .filter(field -> field.appliesTo(target, typeId))
        .toList();
  }

  private static void rebaseSlot(
      NbtPaths.Slot slot, ParcelCoordinateField.Encoding encoding, ParcelSpace space, boolean toWorld) {
    Tag tag = slot.get();
    if (tag == null) {
      return;
    }
    switch (encoding) {
      case BLOCK_POS -> readBlockPos(tag).ifPresent(pos -> {
        BlockPos rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodeBlockPos(rebased));
      });
      case BLOCK_POS_XYZ -> readBlockPosXyz(tag).ifPresent(pos -> {
        BlockPos rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodeBlockPosXyz(rebased));
      });
      case POSITION -> readPosition(tag).ifPresent(pos -> {
        Vec3 rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodePosition(rebased));
      });
    }
  }

  private static Optional<BlockPos> readBlockPos(Tag tag) {
    return BlockPos.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }

  private static Tag encodeBlockPos(BlockPos pos) {
    return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
  }

  private static Optional<BlockPos> readBlockPosXyz(Tag tag) {
    if (!(tag instanceof CompoundTag compound)) {
      return Optional.empty();
    }
    var x = compound.getInt("X");
    var y = compound.getInt("Y");
    var z = compound.getInt("Z");
    if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new BlockPos(x.orElseThrow(), y.orElseThrow(), z.orElseThrow()));
  }

  private static Tag encodeBlockPosXyz(BlockPos pos) {
    var compound = new CompoundTag();
    compound.putInt("X", pos.getX());
    compound.putInt("Y", pos.getY());
    compound.putInt("Z", pos.getZ());
    return compound;
  }

  private static Optional<Vec3> readPosition(Tag tag) {
    return Vec3.CODEC.parse(NbtOps.INSTANCE, tag).result();
  }

  private static Tag encodePosition(Vec3 pos) {
    return Vec3.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow();
  }
}
