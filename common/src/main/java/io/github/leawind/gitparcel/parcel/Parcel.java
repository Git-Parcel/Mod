package io.github.leawind.gitparcel.parcel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class Parcel {
  public final int originX;
  public final int originY;
  public final int originZ;
  public final int sizeX;
  public final int sizeY;
  public final int sizeZ;

  public Parcel(BlockPos origin, Vec3i size) {
    this(origin.getX(), origin.getY(), origin.getZ(), size.getX(), size.getY(), size.getZ());
  }

  public Parcel(int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    this.originX = originX;
    this.originY = originY;
    this.originZ = originZ;
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.sizeZ = sizeZ;
  }

  public BlockPos getOrigin() {
    return new BlockPos(originX, originY, originZ);
  }

  public Vec3i getSize() {
    return new Vec3i(sizeX, sizeY, sizeZ);
  }

  public BlockPos getMaxPos() {
    return new BlockPos(originX + sizeX - 1, originY + sizeY - 1, originZ + sizeZ - 1);
  }

  public Vec3i getEnd() {
    return new Vec3i(originX + sizeX, originY + sizeY, originZ + sizeZ);
  }

  public int getMaxX() {
    return originX + sizeX - 1;
  }

  public int getMaxY() {
    return originY + sizeY - 1;
  }

  public int getMaxZ() {
    return originZ + sizeZ - 1;
  }

  public int getEndX() {
    return originX + sizeX;
  }

  public int getEndY() {
    return originY + sizeY;
  }

  public int getEndZ() {
    return originZ + sizeZ;
  }

  public int getBlockCount() {
    return sizeX * sizeY * sizeZ;
  }

  public BoundingBox getBoundingBox() {
    return new BoundingBox(originX, originY, originZ, getMaxX(), getMaxY(), getMaxZ());
  }

  public AABB getAABB() {
    return new AABB(originX, originY, originZ, getEndX(), getEndY(), getEndZ());
  }

  public static Parcel fromCorners(BlockPos from, BlockPos to) {
    return new Parcel(
        from.getX(),
        from.getY(),
        from.getZ(),
        to.getX() - from.getX() + 1,
        to.getY() - from.getY() + 1,
        to.getZ() - from.getZ() + 1);
  }

  public static Parcel startEnd(Vec3i start, Vec3i end) {
    return new Parcel(
        start.getX(),
        start.getY(),
        start.getZ(),
        end.getX() - start.getX(),
        end.getY() - start.getY(),
        end.getZ() - start.getZ());
  }
}
