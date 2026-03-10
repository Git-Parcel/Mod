package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.parcelformats.parcella.d32.ParcellaD32Format;

public interface ParcellaD16Format extends ParcellaD32Format {

  Info INFO = new Info("parcella_d16", 0);

  @Override
  default Info info() {
    return INFO;
  }
}
