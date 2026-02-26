package io.github.leawind.gitparcel.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public final class Parcel {
  public BlockPos origin;
  public Vec3i size;

  public Parcel(BlockPos origin, Vec3i size) {
    this.origin = origin;
    this.size = size;
  }

  public BlockPos getMaxPos() {
    return new BlockPos(
        origin.getX() + size.getX() - 1,
        origin.getY() + size.getY() - 1,
        origin.getZ() + size.getZ() - 1);
  }

  public BoundingBox getBoundingBox() {
    var maxPos = getMaxPos();
    return new BoundingBox(
        origin.getX(), origin.getY(), origin.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
  }

  public AABB getAABB() {
    return new AABB(
        origin.getX(),
        origin.getY(),
        origin.getZ(),
        origin.getX() + size.getX(),
        origin.getY() + size.getY(),
        origin.getZ() + size.getZ());
  }
}
