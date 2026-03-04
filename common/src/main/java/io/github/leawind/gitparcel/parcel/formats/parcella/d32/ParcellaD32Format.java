package io.github.leawind.gitparcel.parcel.formats.parcella.d32;

import io.github.leawind.gitparcel.parcel.formats.parcella.d16.ParcellaD16Format;

public interface ParcellaD32Format extends ParcellaD16Format {

  @Override
  default String id() {
    return "parcella_d32";
  }
}
