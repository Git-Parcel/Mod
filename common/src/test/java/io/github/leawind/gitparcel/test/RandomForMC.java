package io.github.leawind.gitparcel.test;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public class RandomForMC extends Random {
  public RandomForMC(long seed) {
    super(seed);
  }

  public BlockPos nextBlockPos() {
    return new BlockPos(nextInt(), nextInt(), nextInt());
  }

  public BlockPos nextBlockPos(int min, int max) {
    return new BlockPos(nextInt(min, max), nextInt(min, max), nextInt(min, max));
  }

  public BlockPos nextBlockPos(Vec3i bound) {
    return new BlockPos(nextInt(bound.getX()), nextInt(bound.getY()), nextInt(bound.getZ()));
  }

  public BlockPos nextBlockPos(Vec3i origin, Vec3i bound) {
    return new BlockPos(
        nextInt(origin.getX(), bound.getX()),
        nextInt(origin.getY(), bound.getY()),
        nextInt(origin.getZ(), bound.getZ()));
  }

  public Vec3i nextVec3i() {
    return new Vec3i(nextInt(), nextInt(), nextInt());
  }

  public Vec3i nextVec3i(int min, int max) {
    return new Vec3i(nextInt(min, max), nextInt(min, max), nextInt(min, max));
  }

  public Vec3i nextVec3i(Vec3i bound) {
    return new Vec3i(nextInt(bound.getX()), nextInt(bound.getY()), nextInt(bound.getZ()));
  }

  public Vec3i nextVec3i(Vec3i origin, Vec3i bound) {
    return new Vec3i(
        nextInt(origin.getX(), bound.getX()),
        nextInt(origin.getY(), bound.getY()),
        nextInt(origin.getZ(), bound.getZ()));
  }
}
