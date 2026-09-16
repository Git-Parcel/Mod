package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelExtent;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
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
    transformBlockEntity(context, data, typeId, context.space(), false);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var typeId = record.data().getString("id").map(Identifier::parse).orElse(null);
    var data = record.data().copy();
    transformBlockEntity(context, data, typeId, context.space(), true);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, Entity source, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(context, data, record.type(), context.space(), false);
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    transformEntityTree(context, data, record.type(), context.space(), true);
    return new EntityRecord(record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private static void transformBlockEntity(
      ParcelRecordProcessorContext context,
      CompoundTag data,
      @Nullable Identifier typeId,
      ParcelSpace space,
      boolean toWorld) {
    if (typeId == null) {
      return;
    }
    var fields = applicableFields(context, ParcelCoordinateField.Target.BLOCK_ENTITY, typeId);
    if (fields.isEmpty()) {
      return;
    }
    // Step fields are rewritten before direction fields so they still see the original facing.
    var frameFacing = firstDeclaredDirection(fields, data);
    for (var field : stepFieldsFirst(fields)) {
      NbtPaths.forEach(
          data,
          NbtPaths.parse(field.path()),
          slot -> rebaseSlot(context, field, slot, space, toWorld, frameFacing));
    }
  }

  private static void transformEntityTree(
      ParcelRecordProcessorContext context,
      CompoundTag data,
      Identifier typeId,
      ParcelSpace space,
      boolean toWorld) {
    var fields = applicableFields(context, ParcelCoordinateField.Target.ENTITY, typeId);
    // Step fields are rewritten before direction fields so they still see the original facing.
    var frameFacing = firstDeclaredDirection(fields, data);
    for (var field : stepFieldsFirst(fields)) {
      NbtPaths.forEach(
          data, NbtPaths.parse(field.path()), slot -> rebaseSlot(context, field, slot, space, toWorld, frameFacing));
    }
    data.getList("Passengers")
        .ifPresent(
            passengers ->
                passengers
                    .compoundStream()
                    .forEach(
                        passenger ->
                            transformEntityTree(
                                context,
                                passenger,
                                passenger.getString("id").map(Identifier::parse).orElse(null),
                                space,
                                toWorld)));
  }

  /**
   * Rule 7.3 field participation: capture applies every registered field; restore only the fields
   * the snapshot's self-description records. Restoring a snapshot without a manifest applies none,
   * because whether its declared fields were ever relativized is unknown.
   */
  private static List<ParcelCoordinateField> applicableFields(
      ParcelRecordProcessorContext context,
      ParcelCoordinateField.Target target,
      @Nullable Identifier typeId) {
    boolean capturing = context.collector() != null;
    var semantics = context.semantics();
    return ParcelCoordinateFieldRegistry.get().fields().stream()
        .filter(field -> field.appliesTo(target, typeId))
        .filter(field -> capturing || (semantics != null && semantics.declares(field)))
        .toList();
  }

  private static void rebaseSlot(
      ParcelRecordProcessorContext context,
      ParcelCoordinateField field,
      NbtPaths.Slot slot,
      ParcelSpace space,
      boolean toWorld,
      @Nullable Direction frameFacing) {
    Tag tag = slot.get();
    if (tag == null) {
      return;
    }
    var encoding = field.encoding();
    switch (encoding) {
      case BLOCK_POS -> readBlockPos(tag).ifPresent(pos -> {
        if (!shouldTransform(context, field, space, new Vec3(pos.getX(), pos.getY(), pos.getZ()), toWorld)) {
          return;
        }
        BlockPos rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodeBlockPos(rebased));
      });
      case BLOCK_POS_XYZ -> readBlockPosXyz(tag).ifPresent(pos -> {
        if (!shouldTransform(context, field, space, new Vec3(pos.getX(), pos.getY(), pos.getZ()), toWorld)) {
          return;
        }
        BlockPos rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodeBlockPosXyz(rebased));
      });
      case POSITION -> readPosition(tag).ifPresent(pos -> {
        if (!shouldTransform(context, field, space, pos, toWorld)) {
          return;
        }
        Vec3 rebased = toWorld ? space.toWorld(pos) : space.toParcel(pos);
        slot.set(encodePosition(rebased));
      });
      case DIRECTION -> readDirection(tag).ifPresent(direction -> {
        Direction rebased =
            toWorld ? space.toWorldDirection(direction) : space.toParcelDirection(direction);
        slot.set(encodeDirection(rebased, tag));
      });
      case ROTATION_STEP -> readStep(tag).ifPresent(step -> {
        int rebased =
            frameFacing != null
                ? (toWorld
                    ? space.toWorldRotationStep(frameFacing, step)
                    : space.toParcelRotationStep(frameFacing, step))
                : fallbackStep(space, step);
        slot.set(encodeStep(rebased, tag));
      });
    }
  }

  /**
   * Rule 3.2 inside/outside adjudication for one spatial edge value. Capture sees world-space
   * values (inverse-transform them and test the extent); restore sees parcel-space values (test
   * the extent directly). Without an extent the field degrades to always inside-pointing.
   */
  private static boolean shouldTransform(
      ParcelRecordProcessorContext context,
      ParcelCoordinateField field,
      ParcelSpace space,
      Vec3 value,
      boolean toWorld) {
    if (field.encoding() == ParcelCoordinateField.Encoding.DIRECTION
        || field.encoding() == ParcelCoordinateField.Encoding.ROTATION_STEP) {
      return true;
    }
    switch (field.pointing()) {
      case INSIDE:
        return true;
      case OUTSIDE:
        return false;
      case GEOMETRIC:
        var extent = context.extent();
        return extent == null || extentContains(extent, space, value, toWorld);
    }
    return true;
  }

  private static boolean extentContains(
      ParcelExtent extent, ParcelSpace space, Vec3 value, boolean toWorld) {
    return toWorld ? extent.contains(value) : extent.contains(space.toParcel(value));
  }

  /** Reads the raw value of the first applicable direction field, before any rewrite. */
  private static @Nullable Direction firstDeclaredDirection(
      List<ParcelCoordinateField> fields, CompoundTag data) {
    for (var field : fields) {
      if (field.encoding() != ParcelCoordinateField.Encoding.DIRECTION) {
        continue;
      }
      var found = new Direction[] {null};
      NbtPaths.forEach(
          data,
          NbtPaths.parse(field.path()),
          slot -> {
            if (found[0] == null) {
              found[0] = readDirection(slot.get()).orElse(null);
            }
          });
      if (found[0] != null) {
        return found[0];
      }
    }
    return null;
  }

  /** Rotation steps are transformed first so direction rewrites cannot hide the original facing. */
  private static List<ParcelCoordinateField> stepFieldsFirst(List<ParcelCoordinateField> fields) {
    var steps = fields.stream().filter(f -> f.encoding() == ParcelCoordinateField.Encoding.ROTATION_STEP).toList();
    var others = fields.stream().filter(f -> f.encoding() != ParcelCoordinateField.Encoding.ROTATION_STEP).toList();
    var ordered = new ArrayList<ParcelCoordinateField>(fields.size());
    ordered.addAll(steps);
    ordered.addAll(others);
    return List.copyOf(ordered);
  }

  /** Wall-frame convention for records whose facing is unknown: mirror negates, rotation keeps. */
  private static int fallbackStep(ParcelSpace space, int step) {
    return space.transform().mirror() == net.minecraft.world.level.block.Mirror.NONE
        ? step
        : Math.floorMod(-step, 8);
  }

  private static java.util.Optional<Direction> readDirection(Tag tag) {
    if (tag instanceof StringTag) {
      return Direction.CODEC.parse(NbtOps.INSTANCE, tag).result();
    }
    var data = numericValue(tag);
    return data == null
        ? java.util.Optional.empty()
        : java.util.Optional.of(Direction.from3DDataValue(data));
  }

  private static Tag encodeDirection(Direction direction, Tag original) {
    if (original instanceof StringTag) {
      return StringTag.valueOf(direction.getName());
    }
    int value = direction.get3DDataValue();
    if (original instanceof ByteTag) {
      return ByteTag.valueOf((byte) value);
    }
    if (original instanceof ShortTag) {
      return ShortTag.valueOf((short) value);
    }
    if (original instanceof LongTag) {
      return LongTag.valueOf(value);
    }
    return IntTag.valueOf(value);
  }

  private static java.util.Optional<Integer> readStep(Tag tag) {
    var value = numericValue(tag);
    return value == null ? java.util.Optional.empty() : java.util.Optional.of(value);
  }

  private static Tag encodeStep(int step, Tag original) {
    if (original instanceof ByteTag) {
      return ByteTag.valueOf((byte) step);
    }
    if (original instanceof ShortTag) {
      return ShortTag.valueOf((short) step);
    }
    if (original instanceof LongTag) {
      return LongTag.valueOf(step);
    }
    return IntTag.valueOf(step);
  }

  private static @Nullable Integer numericValue(Tag tag) {
    if (tag instanceof net.minecraft.nbt.NumericTag value) {
      return value.intValue();
    }
    return null;
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
