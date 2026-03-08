package io.github.leawind.gitparcel.parcelformats.parcella;

import io.github.leawind.gitparcel.api.parcel.Parcel;

public class Microparcel extends Parcel implements Parcel.WithValue {
  public int value;

  @Override
  public int getValue() {
    return value;
  }

  public Microparcel(
      int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
    super(originX, originY, originZ, sizeX, sizeY, sizeZ);
    this.value = value;
  }
}
