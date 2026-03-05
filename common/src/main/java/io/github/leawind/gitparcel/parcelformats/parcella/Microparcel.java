package io.github.leawind.gitparcel.parcelformats.parcella;

import io.github.leawind.gitparcel.algorithms.SubdivideAlgo;
import io.github.leawind.gitparcel.api.parcel.Parcel;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;

public class Microparcel extends Parcel implements Parcel.WithValue {
  public int value;

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

  /**
   * Subdivides the given parcel into microparcels.
   *
   * @param parcel the parcel to subdivide
   * @param level the level to read from
   * @param palette the block palette to use
   * @return the list of microparcels
   */
  @Deprecated
  public static ArrayList<Microparcel> subdivide(
      Parcel parcel, LevelReader level, BlockPalette palette) {
    return SubdivideAlgo.INSTANCE.subdivide(
        parcel.sizeX,
        parcel.sizeY,
        parcel.sizeZ,
        (x, y, z) ->
            palette.collect(
                level, new BlockPos(parcel.originX + x, parcel.originY + y, parcel.originZ + z)),
        Microparcel::new);
  }
}
