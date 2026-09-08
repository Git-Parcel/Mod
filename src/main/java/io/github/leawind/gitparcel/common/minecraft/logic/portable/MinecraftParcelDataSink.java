package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.TagValueInput;

/** Places portable parcel records into a server level. */
public final class MinecraftParcelDataSink implements ParcelDataSink {
  private record BufferedEntity(Optional<UUID> originalId, EntityRecord record) {}

  private final ServerLevelAccessor level;
  private final ParcelSpace space;
  private final boolean ignoreBlocks;
  private final boolean ignoreEntities;
  private final int blockUpdateFlags;
  private final int sourceDataVersion;
  private final ProblemReporter.ScopedCollector reporter =
      new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER);
  private final ParcelRecordProcessorContext processorContext;
  private final ParcelAttachmentSession attachments = new ParcelAttachmentSession();
  private final List<BufferedEntity> bufferedEntities = new ArrayList<>();
  private boolean finished;

  public MinecraftParcelDataSink(
      ServerLevelAccessor level,
      ParcelSpace space,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      int blockUpdateFlags,
      int sourceDataVersion) {
    this.level = level;
    this.space = space;
    this.ignoreBlocks = ignoreBlocks;
    this.ignoreEntities = ignoreEntities;
    this.blockUpdateFlags = blockUpdateFlags;
    this.sourceDataVersion = sourceDataVersion;
    this.processorContext = new ParcelRecordProcessorContext(level, space, attachments, null);
  }

  @Override
  public void acceptAttachment(AttachmentRecord attachment) throws ParcelException {
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
      type.restore(processorContext, attachment);
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
      for (var processor : ParcelRecordProcessorRegistry.get().orderedProcessors()) {
        record = processor.restoreBlockEntity(processorContext, record);
      }
      var worldPos = space.toWorld(record.pos());
      var blockEntity = level.getBlockEntity(worldPos);
      if (blockEntity != null) {
        blockEntity.loadWithComponents(
            TagValueInput.create(reporter, level.registryAccess(), record.data()));
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
    for (var processor : ParcelRecordProcessorRegistry.get().orderedProcessors()) {
      record = processor.restoreEntity(processorContext, record);
    }
    bufferedEntities.add(new BufferedEntity(originalId, record));
  }

  /**
   * Summons the whole entity batch with fresh UUIDs, rewriting declared references so intra-parcel
   * links (leashes and mod-owned fields) survive the restore.
   */
  @Override
  public void commit() throws ParcelException {
    if (ignoreEntities) {
      return;
    }
    var remap =
        EntityUuidRemapper.assignFreshIds(
            bufferedEntities.stream().map(BufferedEntity::originalId).flatMap(Optional::stream).toList());
    for (BufferedEntity buffered : bufferedEntities) {
      CompoundTag data = buffered.record().data().copy();
      buffered
          .originalId()
          .ifPresent(id -> data.put("UUID", encodeUuid(remap.getOrDefault(id, id))));
      EntityUuidRemapper.rewriteReferences(data, buffered.record().type(), remap);
      data.putString("id", buffered.record().type().toString());
      var entity =
          EntityType.loadEntityRecursive(
              data, level.getLevel(), EntitySpawnReason.LOAD, EntityProcessor.NOP);
      if (entity == null) {
        throw new ParcelException("Failed to create entity " + buffered.record().type());
      }
      var worldPosition = space.toWorld(buffered.record().pos());
      entity.snapTo(
          worldPosition,
          space.toWorldYaw(entity.getYRot()),
          entity.getXRot());
      level.addFreshEntityWithPassengers(entity);
    }
  }

  private static Optional<UUID> readEntityUuid(CompoundTag data) {
    var uuid = data.getIntArray("UUID");
    if (uuid.isEmpty() || uuid.orElseThrow().length != 4) {
      return Optional.empty();
    }
    int[] parts = uuid.orElseThrow();
    return Optional.of(
        new UUID(
            ((long) parts[0] << 32) | (parts[1] & 0xFFFFFFFFL),
            ((long) parts[2] << 32) | (parts[3] & 0xFFFFFFFFL)));
  }

  private static net.minecraft.nbt.Tag encodeUuid(UUID uuid) {
    return net.minecraft.core.UUIDUtil.CODEC
        .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, uuid)
        .getOrThrow();
  }

  @Override
  public void finish() {
    if (!finished) {
      reporter.close();
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
