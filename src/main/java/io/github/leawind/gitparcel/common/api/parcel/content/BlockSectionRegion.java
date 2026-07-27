package io.github.leawind.gitparcel.common.api.parcel.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Geometry of one grid-aligned block section before its states are captured. */
public record BlockSectionRegion(BlockPos origin, Vec3i size) {
  public BlockSectionRegion {
    origin = origin.immutable();
    size = new Vec3i(size.getX(), size.getY(), size.getZ());
    if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
      throw new IllegalArgumentException("Block section dimensions must be positive: " + size);
    }
  }

  public Vec3i gridCoordinate(int gridSize, Vec3i anchor) {
    if (gridSize <= 0) {
      throw new IllegalArgumentException("Grid size must be positive");
    }
    return new Vec3i(
        Math.floorDiv(origin.getX() - anchor.getX(), gridSize),
        Math.floorDiv(origin.getY() - anchor.getY(), gridSize),
        Math.floorDiv(origin.getZ() - anchor.getZ(), gridSize));
  }

  public BoundingBox bounds() {
    return new BoundingBox(
        origin.getX(),
        origin.getY(),
        origin.getZ(),
        origin.getX() + size.getX() - 1,
        origin.getY() + size.getY() - 1,
        origin.getZ() + size.getZ() - 1);
  }
}
