package io.github.leawind.gitparcel.common.minecraft.logic.transform;

import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.utils.TransformUtils;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies parcel orientation to Minecraft block states.
 *
 * <p>This adapter keeps version-sensitive block APIs out of the serializable parcel transform
 * model.
 */
public final class ParcelBlockTransform {
  private ParcelBlockTransform() {}

  /** Converts a block state from parcel-local orientation to world orientation. */
  public static BlockState toWorldSpace(ParcelTransform transform, BlockState blockState) {
    return blockState.mirror(transform.mirror()).rotate(transform.rotation());
  }

  /** Converts a block state from world orientation to parcel-local orientation. */
  public static BlockState toParcelSpace(ParcelTransform transform, BlockState blockState) {
    return blockState.rotate(TransformUtils.invert(transform.rotation())).mirror(transform.mirror());
  }
}
