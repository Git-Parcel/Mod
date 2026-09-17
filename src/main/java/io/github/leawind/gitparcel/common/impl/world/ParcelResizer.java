package io.github.leawind.gitparcel.common.impl.world;

import io.github.leawind.gitparcel.common.api.parcel.ParcelSpace;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Adjusts a parcel's content extent to a new world box (the {@code resize} use case).
 *
 * <p>The placement transform is untouched: the anchor keeps its absolute world position and the
 * local orientation stays as placed, so only the anchor-relative extent changes (SEMANTICS.md
 * definition 3.1). The operation is pure registration — no world writes, no archive writes — and
 * fully reversible by resizing back.
 */
public final class ParcelResizer {
  private ParcelResizer() {}

  /**
   * Rewrites the parcel's extent to cover {@code newBox} (inclusive corners).
   *
   * <p>The new extent in parcel space is the transform image of the box, so the anchor-relative
   * offset becomes the negative of the transformed minimum corner and the size the transformed
   * span. World content is neither read nor written: cells added by the resize are captured by the
   * next save, and cells removed are reconciled out of the archive by the next save.
   */
  public static void resize(Parcel parcel, BoundingBox newBox) {
    var space = new ParcelSpace(parcel.transform());
    Vec3i min = minCorner(space, newBox);
    Vec3i max = maxCorner(space, newBox);
    Vec3i size =
        new Vec3i(
            max.getX() - min.getX() + 1,
            max.getY() - min.getY() + 1,
            max.getZ() - min.getZ() + 1);
    Vec3i anchorOffset = new Vec3i(-min.getX(), -min.getY(), -min.getZ());
    parcel.meta().resize(size, anchorOffset);
  }

  /** The transform image of the box, smallest corner per axis, in parcel space. */
  private static Vec3i minCorner(ParcelSpace space, BoundingBox box) {
    var a = space.toParcel(new BlockPos(box.minX(), box.minY(), box.minZ()));
    var b = space.toParcel(new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
    return componentMin(a, b);
  }

  /** The transform image of the box, largest corner per axis, in parcel space. */
  private static Vec3i maxCorner(ParcelSpace space, BoundingBox box) {
    var a = space.toParcel(new BlockPos(box.minX(), box.minY(), box.minZ()));
    var b = space.toParcel(new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
    return componentMax(a, b);
  }

  private static Vec3i componentMin(Vec3i a, Vec3i b) {
    return new Vec3i(
        Math.min(a.getX(), b.getX()),
        Math.min(a.getY(), b.getY()),
        Math.min(a.getZ(), b.getZ()));
  }

  private static Vec3i componentMax(Vec3i a, Vec3i b) {
    return new Vec3i(
        Math.max(a.getX(), b.getX()),
        Math.max(a.getY(), b.getY()),
        Math.max(a.getZ(), b.getZ()));
  }
}
