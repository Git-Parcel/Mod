package io.github.leawind.gitparcel.parcel.formats.parcella;

import io.github.leawind.gitparcel.algorithms.SubdivideAlgo;
import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public class Microparcel extends Parcel implements Parcel.WithValue {
  int value;

  @Override
  public int getValue() {
    return value;
  }

  public Microparcel(int value, BlockPos origin, Vec3i size) {
    super(origin, size);
    this.value = value;
  }

  public Microparcel(
      int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    super(originX, originY, originZ, sizeX, sizeY, sizeZ);
    this.value = value;
  }

  public static ArrayList<Microparcel> subdivide(Parcel parcel, Level level, BlockPalette palette) {
    return SubdivideAlgo.V3.subdivide(parcel, pos -> palette.collect(level, pos), Microparcel::new);
  }
}
