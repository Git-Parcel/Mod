package io.github.leawind.gitparcel.common.impl.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.ScheduledTickRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.SemanticData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.TickPriority;

public final class ParcelRecordCodecs {
  private ParcelRecordCodecs() {}

  public static final Codec<SemanticData> SEMANTIC_DATA =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Identifier.CODEC.fieldOf("processor").forGetter(SemanticData::processor),
                      Codec.INT.fieldOf("schema_version").forGetter(SemanticData::schemaVersion),
                      CompoundTag.CODEC.fieldOf("payload").forGetter(SemanticData::payload))
                  .apply(inst, SemanticData::new));

  public static final Codec<BlockEntityRecord> BLOCK_ENTITY =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      BlockPos.CODEC.fieldOf("pos").forGetter(BlockEntityRecord::pos),
                      CompoundTag.CODEC.fieldOf("data").forGetter(BlockEntityRecord::data),
                      SEMANTIC_DATA
                          .listOf()
                          .optionalFieldOf("semantic_data", List.of())
                          .forGetter(BlockEntityRecord::semanticData))
                  .apply(inst, BlockEntityRecord::new));

  public record BlockEntities(List<BlockEntityRecord> entries) {}

  public static final Codec<BlockEntities> BLOCK_ENTITIES =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      BLOCK_ENTITY.listOf().fieldOf("entries").forGetter(BlockEntities::entries))
                  .apply(inst, BlockEntities::new));

  /**
   * Mirrors vanilla chunk tick storage (SavedTick): the trigger tick travels as a delay relative to
   * the capture game time and sub-tick ordering is not persisted.
   */
  private static final Codec<ScheduledTickRecord> SCHEDULED_TICK =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.BOOL
                          .optionalFieldOf("fluid", false)
                          .forGetter(ScheduledTickRecord::fluid),
                      Identifier.CODEC.fieldOf("i").forGetter(ScheduledTickRecord::typeId),
                      BlockPos.CODEC.fieldOf("pos").forGetter(ScheduledTickRecord::pos),
                      Codec.INT.fieldOf("t").forGetter(ScheduledTickRecord::delay),
                      /*? if >=26.1 {*/
                      TickPriority.CODEC
                          .optionalFieldOf("p", TickPriority.NORMAL)
                          .forGetter(ScheduledTickRecord::priority))
                  .apply(inst, ScheduledTickRecord::new));
      /*?} else {*/
      /*Codec.INT
                  .xmap(TickPriority::byValue, TickPriority::getValue)
                  .optionalFieldOf("p", TickPriority.NORMAL)
                  .forGetter(ScheduledTickRecord::priority))
          .apply(inst, ScheduledTickRecord::new));
      *//*?}*/

  public record ScheduledTicks(List<ScheduledTickRecord> entries) {}

  public static final Codec<ScheduledTicks> SCHEDULED_TICKS =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      SCHEDULED_TICK
                          .listOf()
                          .fieldOf("entries")
                          .forGetter(ScheduledTicks::entries))
                  .apply(inst, ScheduledTicks::new));

  public static final Codec<EntityRecord> ENTITY =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Identifier.CODEC.fieldOf("type").forGetter(EntityRecord::type),
                      Vec3.CODEC.fieldOf("pos").forGetter(EntityRecord::pos),
                      BlockPos.CODEC.fieldOf("block_pos").forGetter(EntityRecord::blockPos),
                      CompoundTag.CODEC.fieldOf("data").forGetter(EntityRecord::data),
                      SEMANTIC_DATA
                          .listOf()
                          .optionalFieldOf("semantic_data", List.of())
                          .forGetter(EntityRecord::semanticData))
                  .apply(inst, EntityRecord::new));

  private static final Codec<LocalAttachmentId> ATTACHMENT_ID =
      Codec.STRING.xmap(LocalAttachmentId::new, LocalAttachmentId::value);

  public static final Codec<AttachmentRecord> ATTACHMENT =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ATTACHMENT_ID.fieldOf("id").forGetter(AttachmentRecord::id),
                      Identifier.CODEC.fieldOf("type").forGetter(AttachmentRecord::type),
                      Codec.INT.fieldOf("schema_version").forGetter(AttachmentRecord::schemaVersion),
                      Codec.BOOL.optionalFieldOf("required", true)
                          .forGetter(AttachmentRecord::required),
                      CompoundTag.CODEC.fieldOf("payload").forGetter(AttachmentRecord::payload))
                  .apply(inst, AttachmentRecord::new));

  public static CompoundTag encode(Codec<?> codec, Object value) {
    @SuppressWarnings("unchecked")
    Codec<Object> typed = (Codec<Object>) codec;
    return (CompoundTag) typed.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, value).result().orElseThrow();
  }

  public static <T> T decode(Codec<T> codec, CompoundTag tag) {
    return codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag).result().orElseThrow();
  }
}
