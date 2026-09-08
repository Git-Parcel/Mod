package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContext;
import io.github.leawind.gitparcel.common.api.extension.contributor.ParcelCaptureContributorRegistry;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorRegistry;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.impl.parcel.BlockSectionPartitioner;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.minecraft.logic.transform.ParcelBlockTransform;
import io.github.leawind.gitparcel.common.impl.extension.attachment.ParcelAttachmentSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Lazily captures a level into portable parcel records. */
public final class MinecraftParcelDataSource implements ParcelDataSource {
  private final Level level;
  private final Vec3i size;
  private final Vec3i anchor;
  private final ParcelSpace space;
  private final boolean ignoreEntities;
  private final ParcelAttachmentSession attachments = new ParcelAttachmentSession();

  public MinecraftParcelDataSource(
      Level level, Vec3i size, Vec3i anchor, ParcelSpace space, boolean ignoreEntities) {
    this.level = level;
    this.size = size;
    this.anchor = anchor;
    this.space = space;
    this.ignoreEntities = ignoreEntities;
  }

  @Override
  public void forEachBlockSection(
      int sectionSize, ParcelDataConsumer<BlockSection> consumer)
      throws IOException, ParcelException {
    var processorContext = new ParcelRecordProcessorContext(level, space, attachments, attachments);
    var processors = ParcelRecordProcessorRegistry.get().orderedProcessors();
    for (var section : BlockSectionPartitioner.partition(size, anchor, sectionSize)) {
      var states =
          new ArrayList<net.minecraft.world.level.block.state.BlockState>(
              section.size().getX() * section.size().getY() * section.size().getZ());
      var blockEntities = new ArrayList<BlockEntityRecord>();
      BlockPos relativeOrigin =
          new BlockPos(
              section.origin().getX() - anchor.getX(),
              section.origin().getY() - anchor.getY(),
              section.origin().getZ() - anchor.getZ());

      for (int x = 0; x < section.size().getX(); x++) {
        for (int y = 0; y < section.size().getY(); y++) {
          for (int z = 0; z < section.size().getZ(); z++) {
            BlockPos relativePos = relativeOrigin.offset(x, y, z);
            BlockPos worldPos = space.toWorld(relativePos);
            states.add(
                ParcelBlockTransform.toParcelSpace(
                    space.transform(), level.getBlockState(worldPos)));
            BlockEntity blockEntity = level.getBlockEntity(worldPos);
            if (blockEntity != null) {
              CompoundTag data = blockEntity.saveWithFullMetadata(level.registryAccess());
              BlockEntityRecord record =
                  new BlockEntityRecord(relativePos, data, List.of());
              for (var processor : processors) {
                record =
                    processor.captureBlockEntity(processorContext, blockEntity, record);
              }
              blockEntities.add(record);
            }
          }
        }
      }
      consumer.accept(
          new BlockSection(
              relativeOrigin,
              section.size(),
              states,
              blockEntities));
    }
  }

  @Override
  public void forEachEntity(ParcelDataConsumer<EntityRecord> consumer)
      throws IOException, ParcelException {
    if (ignoreEntities) {
      return;
    }
    var processorContext = new ParcelRecordProcessorContext(level, space, attachments, attachments);
    var processors = ParcelRecordProcessorRegistry.get().orderedProcessors();
    AABB bounds = worldBounds();
    List<Entity> entities =
        level.getEntities(
            (Entity) null,
            bounds,
            entity -> !(entity instanceof Player) && !entity.isPassenger());
    entities.sort(Comparator.comparing(Entity::getUUID));
    try (var reporter = new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER)) {
      for (Entity entity : entities) {
        var output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
        if (!entity.save(output)) {
          continue;
        }
        Vec3 pos = space.toParcel(entity.position());
        BlockPos attached =
            entity instanceof Painting painting
                ? space.toParcel(painting.getPos())
                : BlockPos.containing(pos);
        EntityRecord record =
            new EntityRecord(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()),
                pos,
                attached,
                output.buildResult().copy(),
                List.of());
        for (var processor : processors) {
          record = processor.captureEntity(processorContext, entity, record);
        }
        consumer.accept(record);
      }
    }
  }

  @Override
  public void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer)
      throws IOException, ParcelException {
    var contributorContext = new ParcelCaptureContext(level, space, worldBounds(), attachments);
    for (var contributor : ParcelCaptureContributorRegistry.get().contributors()) {
      try {
        contributor.capture(contributorContext);
      } catch (Exception e) {
        throw new ParcelException(
            "Parcel capture contributor failed: " + contributor.id(), e);
      }
    }
    for (AttachmentRecord attachment : attachments.captured()) {
      consumer.accept(attachment);
    }
  }

  private AABB worldBounds() {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;
    double maxZ = Double.NEGATIVE_INFINITY;
    int[] xs = {-anchor.getX(), size.getX() - anchor.getX()};
    int[] ys = {-anchor.getY(), size.getY() - anchor.getY()};
    int[] zs = {-anchor.getZ(), size.getZ() - anchor.getZ()};
    for (int x : xs) {
      for (int y : ys) {
        for (int z : zs) {
          Vec3 point = space.toWorld(new Vec3(x, y, z));
          minX = Math.min(minX, point.x);
          minY = Math.min(minY, point.y);
          minZ = Math.min(minZ, point.z);
          maxX = Math.max(maxX, point.x);
          maxY = Math.max(maxY, point.y);
          maxZ = Math.max(maxZ, point.z);
        }
      }
    }
    return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
  }
}
