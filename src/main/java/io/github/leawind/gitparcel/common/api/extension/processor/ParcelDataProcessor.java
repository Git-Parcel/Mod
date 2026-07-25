package io.github.leawind.gitparcel.common.api.extension.processor;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Transforms Minecraft data into and out of portable parcel records.
 *
 * <p>Implementations must be stateless. Methods are invoked on the server thread.
 */
public interface ParcelDataProcessor {
  Identifier id();

  default Set<Identifier> runAfter() {
    return Set.of();
  }

  default Set<Identifier> runBefore() {
    return Set.of();
  }

  default BlockEntityRecord captureBlockEntity(
      ParcelProcessorContext context, BlockEntity source, BlockEntityRecord record)
      throws ParcelException {
    return record;
  }

  default BlockEntityRecord restoreBlockEntity(
      ParcelProcessorContext context, BlockEntityRecord record) throws ParcelException {
    return record;
  }

  default EntityRecord captureEntity(
      ParcelProcessorContext context, Entity source, EntityRecord record) throws ParcelException {
    return record;
  }

  default EntityRecord restoreEntity(
      ParcelProcessorContext context, EntityRecord record) throws ParcelException {
    return record;
  }
}
