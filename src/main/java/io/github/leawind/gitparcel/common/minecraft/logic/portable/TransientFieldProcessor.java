package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientField;
import io.github.leawind.gitparcel.common.api.extension.transientfield.ParcelTransientFieldRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Removes or re-anchors the declared transient fields (SEMANTICS.md definition 2.5) after the core
 * processor has rewritten the authoritative positioning, per invariant 6.1.
 *
 * <p>Eliminated fields are removed on capture only; restores fall back to engine defaults because
 * snapshots never contain the fields. Offset fields travel as {@code value - gameTime} and are
 * re-anchored on restore (rule 2.3). Participation follows the snapshot self-description per rule
 * 7.3, so pre-transient snapshots keep their stored absolute values untouched.
 */
public final class TransientFieldProcessor implements ParcelRecordProcessor {
  public static final Identifier ID =
      Identifier.fromNamespaceAndPath("gitparcel", "transient_fields");

  @Override
  public Identifier id() {
    return ID;
  }

  @Override
  public java.util.Set<Identifier> runAfter() {
    return java.util.Set.of(MinecraftCoreRecordProcessor.ID, DeclaredCoordinateFieldProcessor.ID);
  }

  @Override
  public BlockEntityRecord captureBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var data = record.data().copy();
    apply(
        context,
        ParcelTransientField.Target.BLOCK_ENTITY,
        typeId(data),
        data,
        true);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public BlockEntityRecord restoreBlockEntity(
      ParcelRecordProcessorContext context, BlockEntityRecord record) {
    var data = record.data().copy();
    apply(
        context,
        ParcelTransientField.Target.BLOCK_ENTITY,
        typeId(data),
        data,
        false);
    return new BlockEntityRecord(record.pos(), data, record.semanticData());
  }

  @Override
  public EntityRecord captureEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    applyEntityTree(context, data, record.type(), true);
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  @Override
  public EntityRecord restoreEntity(
      ParcelRecordProcessorContext context, EntityRecord record) {
    var data = record.data().copy();
    applyEntityTree(context, data, record.type(), false);
    return new EntityRecord(
        record.type(), record.pos(), record.blockPos(), data, record.semanticData());
  }

  private void applyEntityTree(
      ParcelRecordProcessorContext context,
      CompoundTag data,
      Identifier typeId,
      boolean capturing) {
    apply(context, ParcelTransientField.Target.ENTITY, typeId, data, capturing);
    data.getList("Passengers")
        .ifPresent(
            passengers ->
                passengers
                    .compoundStream()
                    .forEach(
                        passenger ->
                            applyEntityTree(
                                context,
                                passenger,
                                passenger.getString("id").map(Identifier::parse).orElse(null),
                                capturing)));
  }

  private void apply(
      ParcelRecordProcessorContext context,
      ParcelTransientField.Target target,
      @Nullable Identifier typeId,
      CompoundTag data,
      boolean capturing) {
    for (var field : applicable(context, target, typeId, capturing)) {
      switch (field.kind()) {
        case ELIMINATE -> {
          if (capturing) {
            NbtPaths.forEach(data, NbtPaths.parse(field.path()), NbtPaths.Slot::remove);
          }
        }
        case OFFSET_GAME_TIME -> {
          long delta = capturing ? -context.gameTime() : context.gameTime();
          NbtPaths.forEach(
              data,
              NbtPaths.parse(field.path()),
              slot -> offsetValue(slot, delta));
        }
      }
    }
  }

  private static List<ParcelTransientField> applicable(
      ParcelRecordProcessorContext context,
      ParcelTransientField.Target target,
      @Nullable Identifier typeId,
      boolean capturing) {
    var semantics = context.semantics();
    return ParcelTransientFieldRegistry.get().fields().stream()
        .filter(field -> field.appliesTo(target, typeId))
        .filter(field -> capturing || (semantics != null && semantics.declares(field)))
        .toList();
  }

  private static void offsetValue(NbtPaths.Slot slot, long delta) {
    Tag tag = slot.get();
    if (tag instanceof net.minecraft.nbt.NumericTag number) {
      slot.set(LongTag.valueOf(number.longValue() + delta));
    }
  }

  private static @Nullable Identifier typeId(CompoundTag data) {
    return data.getString("id").map(Identifier::parse).orElse(null);
  }

}