package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentRestoreContext;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributorRegistry;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelRestoreContext;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessor;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.ParcelExtent;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSemantics;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ScheduledTickRecord;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.transform.ParcelBlockTransform;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftDataMigration;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
/*? if >=26.1 {*/
import net.minecraft.util.ProblemReporter;
/*?}*/
/*? if >=26.1 {*/
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
/*?}*/
import net.minecraft.world.entity.EntityType;
/*? if >=26.3 {*/
import net.minecraft.world.entity.EntitySpawnRequest;
/*?}*/
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
/*? if >=26.1 {*/
import net.minecraft.world.level.storage.TagValueInput;
/*?}*/
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Places portable parcel records into a server level. */
public final class MinecraftParcelDataSink implements ParcelDataSink {
  private record BufferedEntity(Optional<UUID> originalId, EntityRecord record) {}

  /**
   * Restore-side attachment context: the neutral session plus the target level, exposing the
   * live-world seam that attachment materialization requires.
   */
  private record AttachmentRestoreContext(ServerLevel level, ParcelAttachmentSession attachments)
      implements ParcelAttachmentRestoreContext {
    @Override
    public ServerLevel level() {
      return level;
    }

    @Override
    public void resolve(LocalAttachmentId id, Object value) {
      attachments.resolve(id, value);
    }
  }

  private final ServerLevel level;
  private final ParcelSpace space;
  private final boolean ignoreBlocks;
  private final boolean ignoreEntities;
  private final int blockUpdateFlags;
  private final int sourceDataVersion;
  /*? if >=26.1 {*/
  private final ProblemReporter.ScopedCollector reporter =
      new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER);
  /*?}*/
  private final ParcelRecordProcessorContext processorContext;
  private final ParcelAttachmentSession attachments = new ParcelAttachmentSession();
  private final List<BufferedEntity> bufferedEntities = new ArrayList<>();
  private final List<ScheduledTickRecord> bufferedTicks = new ArrayList<>();
  private final List<AttachmentRecord> restoredAttachments = new ArrayList<>();
  private final List<ParcelRecordProcessor> participants;
  private final List<ParcelEntityRefField> declaredRefFields;
  private boolean finished;

  public MinecraftParcelDataSink(
      ServerLevel level,
      ParcelSpace space,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      int blockUpdateFlags,
      int sourceDataVersion,
      @Nullable ParcelSemantics semantics,
      @Nullable ParcelExtent extent) {
    this.level = level;
    this.space = space;
    this.ignoreBlocks = ignoreBlocks;
    this.ignoreEntities = ignoreEntities;
    this.blockUpdateFlags = blockUpdateFlags;
    this.sourceDataVersion = sourceDataVersion;
    this.participants = participants(semantics);
    this.declaredRefFields = declaredRefFields(semantics);
    this.processorContext =
        new ParcelRecordProcessorContext(
            space, attachments, null, semantics, extent, level.getGameTime());
  }

  /**
   * Resolves the rule 7.3 participants: the processors recorded by the snapshot's
   * self-description. The core processor always participates because authoritative positioning is
   * rewritten unconditionally (SEMANTICS.md definition 2.2); without a manifest only the core
   * processor runs.
   */
  private static List<ParcelRecordProcessor> participants(@Nullable ParcelSemantics semantics) {
    return ParcelRecordProcessorRegistry.get().orderedProcessors().stream()
        .filter(
            processor ->
                processor.id().equals(MinecraftCoreRecordProcessor.ID)
                    || (semantics != null && semantics.declaresProcessor(processor.id())))
        .toList();
  }

  /** Declared entity-reference fields recorded by the snapshot; none without a manifest. */
  private static List<ParcelEntityRefField> declaredRefFields(@Nullable ParcelSemantics semantics) {
    if (semantics == null) {
      return List.of();
    }
    return ParcelEntityRefFieldRegistry.get().fields().stream()
        .filter(semantics::declares)
        .toList();
  }

  @Override
  public void acceptAttachment(AttachmentRecord attachment) throws ParcelException {
    restoredAttachments.add(attachment);
    var type = ParcelAttachmentTypeRegistry.get().get(attachment.type());
    if (type == null) {
      if (attachment.required()) {
        throw new ParcelException(
            "Missing required parcel attachment type: " + attachment.type());
      }
      ParcelStorage.LOGGER.warn("Skipping optional parcel attachment {}", attachment.id());
      return;
    }
    if (type.schemaVersion() != attachment.schemaVersion()) {
      throw new ParcelException(
          "Unsupported schema version %d for attachment %s"
              .formatted(attachment.schemaVersion(), attachment.type()));
    }
    try {
      type.restore(new AttachmentRestoreContext(level, attachments), attachment);
    } catch (Exception e) {
      throw new ParcelException("Failed to restore attachment " + attachment.id(), e);
    }
  }

  @Override
  public void acceptBlockSection(BlockSection section) throws ParcelException {
    if (ignoreBlocks) {
      return;
    }
    for (int x = 0; x < section.size().getX(); x++) {
      for (int y = 0; y < section.size().getY(); y++) {
        for (int z = 0; z < section.size().getZ(); z++) {
          var relativePos = section.origin().offset(x, y, z);
          var worldPos = space.toWorld(relativePos);
          var state =
              ParcelBlockTransform.toWorldSpace(space.transform(), section.state(x, y, z));
          level.getChunk(worldPos).removeBlockEntity(worldPos);
          level.setBlock(worldPos, state, blockUpdateFlags);
        }
      }
    }
    for (BlockEntityRecord original : section.blockEntities()) {
      BlockEntityRecord record =
          new BlockEntityRecord(
              original.pos(),
              MinecraftDataMigration.update(
                  level,
                  MinecraftDataMigration.Kind.BLOCK_ENTITY,
                  original.data(),
                  sourceDataVersion),
              original.semanticData());
      validateSemanticData(record.semanticData());
      for (var processor : participants) {
        record = processor.restoreBlockEntity(processorContext, record);
      }
      var worldPos = space.toWorld(record.pos());
      var blockEntity = level.getBlockEntity(worldPos);
      if (blockEntity != null) {
        /*? if >=26.1 {*/
        blockEntity.loadWithComponents(
            TagValueInput.create(reporter, level.registryAccess(), record.data()));
        /*?} else {*/
        /*blockEntity.load(record.data());
        *//*?}*/
        blockEntity.setChanged();
      }
    }
  }

  @Override
  public void acceptEntity(EntityRecord original) throws ParcelException {
    if (ignoreEntities) {
      return;
    }
    Optional<UUID> originalId = readEntityUuid(original.data());
    EntityRecord record =
        new EntityRecord(
            original.type(),
            original.pos(),
            original.blockPos(),
            MinecraftDataMigration.update(
                level,
                MinecraftDataMigration.Kind.ENTITY_TREE,
                original.data(),
                sourceDataVersion),
            original.semanticData());
    validateSemanticData(record.semanticData());
    for (var processor : participants) {
      record = processor.restoreEntity(processorContext, record);
    }
    bufferedEntities.add(new BufferedEntity(originalId, record));
  }

  @Override
  public void acceptScheduledTick(ScheduledTickRecord tick) throws ParcelException {
    if (!ignoreBlocks) {
      bufferedTicks.add(tick);
    }
  }

  /**
   * Summons the whole entity batch with fresh UUIDs, rewriting declared references so intra-parcel
   * links (leashes and mod-owned fields) survive the restore, replays the snapshot's scheduled
   * ticks, then lets capture contributors re-apply their regional data.
   */
  @Override
  public void commit() throws ParcelException {
    if (!ignoreEntities) {
      var remap =
          EntityUuidRemapper.assignFreshIds(
              bufferedEntities.stream()
                  .map(BufferedEntity::originalId)
                  .flatMap(Optional::stream)
                  .toList());
      for (BufferedEntity buffered : bufferedEntities) {
        CompoundTag data = buffered.record().data().copy();
        buffered
            .originalId()
            .ifPresent(id -> data.put("UUID", encodeUuid(remap.getOrDefault(id, id))));
        EntityUuidRemapper.rewriteReferences(
            data, buffered.record().type(), remap, declaredRefFields);
        data.putString("id", buffered.record().type().toString());
        var entity =
            /*? if >=26.3 {*/
            EntityType.loadEntityRecursive(
                data,
                level.getLevel(),
                new EntitySpawnRequest(EntitySpawnReason.LOAD, false),
                EntityProcessor.NOP);
            /*?} else if >=26.1 {*/
            /*EntityType.loadEntityRecursive(
                data, level.getLevel(), EntitySpawnReason.LOAD, EntityProcessor.NOP);
            *//*?} else {*/
            /*EntityType.loadEntityRecursive(data, level.getLevel(), e -> e);
            *//*?}*/
        if (entity == null) {
          throw new ParcelException("Failed to create entity " + buffered.record().type());
        }
        var worldPosition = space.toWorld(buffered.record().pos());
        /*? if >=26.1 {*/
        entity.snapTo(
            worldPosition,
            space.toWorldYaw(entity.getYRot()),
            entity.getXRot());
        /*?} else {*/
        /*entity.moveTo(
            worldPosition.x,
            worldPosition.y,
            worldPosition.z,
            space.toWorldYaw(entity.getYRot()),
            entity.getXRot());
        *//*?}*/
        level.addFreshEntityWithPassengers(entity);
      }
    }
    if (!ignoreBlocks) {
      replayScheduledTicks();
    }
    var contributorContext = new ParcelRestoreContext(level, space, List.copyOf(restoredAttachments));
    for (var contributor : ParcelCaptureContributorRegistry.get().contributors()) {
      try {
        contributor.restore(contributorContext);
      } catch (Exception e) {
        throw new ParcelException("Parcel restore contributor failed: " + contributor.id(), e);
      }
    }
  }

  /**
   * Re-anchors the buffered ticks onto the current game time (rule 2.3) and schedules them over
   * whatever the block placement itself queued at the same position (rule 6.3): the snapshot is
   * authoritative, so same-position ticks are replaced rather than accumulated.
   */
  private void replayScheduledTicks() throws ParcelException {
    long gameTime = level.getLevel().getGameTime();
    long subTickOrder = -bufferedTicks.size();
    for (ScheduledTickRecord tick : bufferedTicks) {
      BlockPos worldPos = space.toWorld(tick.pos());
      long triggerTick = gameTime + tick.delay();
      if (tick.fluid()) {
        /*? if >=26.1 {*/
        var fluid =
            BuiltInRegistries.FLUID.get(tick.typeId()).map(Holder::value).orElse(null);
        /*?} else {*/
        /*var fluid = BuiltInRegistries.FLUID.get(tick.typeId());
        *//*?}*/
        if (fluid == null) {
          throw new ParcelException.CorruptedParcelException(
              "Unknown fluid in scheduled tick: " + tick.typeId());
        }
        placeTick(
            levelTicks(level.getChunk(worldPos).getFluidTicks()),
            fluid,
            worldPos,
            triggerTick,
            tick.priority(),
            subTickOrder++);
      } else {
        /*? if >=26.1 {*/
        var block =
            BuiltInRegistries.BLOCK.get(tick.typeId()).map(Holder::value).orElse(null);
        /*?} else {*/
        /*var block = BuiltInRegistries.BLOCK.get(tick.typeId());
        *//*?}*/
        if (block == null) {
          throw new ParcelException.CorruptedParcelException(
              "Unknown block in scheduled tick: " + tick.typeId());
        }
        placeTick(
            levelTicks(level.getChunk(worldPos).getBlockTicks()),
            block,
            worldPos,
            triggerTick,
            tick.priority(),
            subTickOrder++);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> LevelChunkTicks<T> levelTicks(
      net.minecraft.world.ticks.TickContainerAccess<T> container) {
    return (LevelChunkTicks<T>) container;
  }

  private <T> void placeTick(
      LevelChunkTicks<T> container,
      T type,
      BlockPos worldPos,
      long triggerTick,
      TickPriority priority,
      long subTickOrder) {
    container.removeIf(existing -> existing.type() == type && existing.pos().equals(worldPos));
    container.schedule(
        new ScheduledTick<>(type, worldPos, triggerTick, priority, subTickOrder));
  }

  private static Optional<UUID> readEntityUuid(CompoundTag data) {
    /*? if >=26.1 {*/
    var uuid = data.getIntArray("UUID");
    if (uuid.isEmpty() || uuid.orElseThrow().length != 4) {
      return Optional.empty();
    }
    int[] parts = uuid.orElseThrow();
    /*?} else {*/
    /*if (!data.contains("UUID") || data.getIntArray("UUID").length != 4) {
      return Optional.empty();
    }
    int[] parts = data.getIntArray("UUID");
    *//*?}*/
    return Optional.of(
        new UUID(
            ((long) parts[0] << 32) | (parts[1] & 0xFFFFFFFFL),
            ((long) parts[2] << 32) | (parts[3] & 0xFFFFFFFFL)));
  }

  private static net.minecraft.nbt.Tag encodeUuid(UUID uuid) {
    return net.minecraft.core.UUIDUtil.CODEC
        .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, uuid)
        .result().orElseThrow();
  }

  @Override
  public void finish() {
    if (!finished) {
      /*? if >=26.1 {*/
      reporter.close();
      /*?}*/
      finished = true;
    }
  }

  private static void validateSemanticData(
      java.util.List<io.github.leawind.gitparcel.common.api.parcel.content.SemanticData> semantics)
      throws ParcelException {
    for (var semantic : semantics) {
      if (ParcelRecordProcessorRegistry.get().get(semantic.processor()) == null) {
        throw new ParcelException(
            "Missing required parcel data processor: " + semantic.processor());
      }
    }
  }
}
