package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.processor.ParcelRecordProcessorContext;
import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Rule 3.2 inside/outside adjudication and the single rebase step shared by every spatial-edge
 * consumer: capture sees world-space values (inverse-transform them into the extent), restore
 * sees parcel-space values (test them directly), and outside-pointing edges keep their stored
 * value on both sides. Without an extent the edge degrades to always inside-pointing.
 */
final class SpatialEdges {
  private SpatialEdges() {}

  /** Whether a geometric spatial edge with this value participates in the transform. */
  static boolean shouldRebase(
      ParcelRecordProcessorContext context, ParcelSpace space, Vec3 value, boolean toWorld) {
    var extent = context.extent();
    return extent == null || (toWorld ? extent.contains(value) : extent.contains(space.toParcel(value)));
  }

  /** Applies the full placement transform in one direction (rule 3.3). */
  static BlockPos rebase(ParcelSpace space, BlockPos pos, boolean toWorld) {
    return toWorld ? space.toWorld(pos) : space.toParcel(pos);
  }
}
