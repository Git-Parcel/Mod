package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.attachment.ParcelAttachmentTypeRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelDataProcessorRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSink;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.transform.ParcelBlockTransform;
import io.github.leawind.gitparcel.common.minecraft.logic.version.MinecraftDataMigration;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.TagValueInput;

/** Places portable parcel records into a server level. */
public final class MinecraftParcelContentSink implements ParcelContentSink {
  private final ServerLevelAccessor level;
  private final ParcelSpace space;
  private final boolean ignoreBlocks;
  private final boolean ignoreEntities;
  private final int blockUpdateFlags;
  private final int sourceDataVersion;
  private final ProblemReporter.ScopedCollector reporter =
      new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER);
  private final ParcelProcessorContext processorContext;
  private final ParcelAttachmentSession attachments = new ParcelAttachmentSession();
  private boolean finished;

  public MinecraftParcelContentSink(
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
    this.processorContext = new ParcelProcessorContext(level, space, attachments);
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
      for (var processor : ParcelDataProcessorRegistry.get().orderedProcessors()) {
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
    for (var processor : ParcelDataProcessorRegistry.get().orderedProcessors()) {
      record = processor.restoreEntity(processorContext, record);
    }
    CompoundTag data = record.data().copy();
    data.putString("id", record.type().toString());
    var entity =
        EntityType.loadEntityRecursive(
            data, level.getLevel(), EntitySpawnReason.LOAD, EntityProcessor.NOP);
    if (entity == null) {
      throw new ParcelException("Failed to create entity " + record.type());
    }
    level.addFreshEntityWithPassengers(entity);
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
      if (ParcelDataProcessorRegistry.get().get(semantic.processor()) == null) {
        throw new ParcelException(
            "Missing required parcel data processor: " + semantic.processor());
      }
    }
  }
}
