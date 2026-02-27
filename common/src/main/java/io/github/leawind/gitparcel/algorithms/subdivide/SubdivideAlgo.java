package io.github.leawind.gitparcel.algorithms.subdivide;

import io.github.leawind.gitparcel.parcel.Parcel;
import java.util.ArrayList;
import java.util.function.Function;
import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface SubdivideAlgo {

  <T extends Parcel & Parcel.WithValue> ArrayList<T> subdivide(
      Parcel parcel, Function<BlockPos, Integer> values, ResultFactory<T> factory);

  interface ResultFactory<T> {
    T create(int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ);
  }

  class ParcelWithValue extends Parcel implements Parcel.WithValue {
    private final int value;

    public ParcelWithValue(
        int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
      super(originX, originY, originZ, sizeX, sizeY, sizeZ);
      this.value = value;
    }

    @Override
    public int getValue() {
      return value;
    }
  }
}
