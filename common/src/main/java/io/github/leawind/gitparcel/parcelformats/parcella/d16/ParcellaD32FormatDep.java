package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Format;

public interface ParcellaD32FormatDep extends ParcellaD32Format {

  @Override
  default String id() {
    return "parcella_d16";
  }
}
