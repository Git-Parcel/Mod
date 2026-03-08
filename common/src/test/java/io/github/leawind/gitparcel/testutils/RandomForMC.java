package io.github.leawind.gitparcel.testutils;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

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

  public Vec3 nextVec3() {
    return new Vec3(nextDouble(), nextDouble(), nextDouble());
  }

  public Vec3 nextVec3(double min, double max) {
    return new Vec3(nextDouble(min, max), nextDouble(min, max), nextDouble(min, max));
  }

  public Vec3 nextVec3(Vec3i bound) {
    return new Vec3(nextDouble(bound.getX()), nextDouble(bound.getY()), nextDouble(bound.getZ()));
  }

  public Vec3 nextVec3(Vec3 origin, Vec3 bound) {
    return new Vec3(
        nextDouble(origin.x, bound.x),
        nextDouble(origin.y, bound.y),
        nextDouble(origin.z, bound.z));
  }

  public <E extends Enum<E>> E nextEnum(Class<E> enumClass) {
    return enumClass.getEnumConstants()[nextInt(enumClass.getEnumConstants().length)];
  }

  public <T> void shuffle(T[] array) {
    for (int i = array.length - 1; i > 0; i--) {
      int index = nextInt(i + 1);
      T temp = array[index];
      array[index] = array[i];
      array[i] = temp;
    }
  }
}
