package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaDigitCodec;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaReader;

public final class ParcellaD32Reader extends ParcellaReader implements ParcellaD32Format {
  public ParcellaD32Reader() {
    super(32, ParcellaDigitCodec.BASE32);
  }
}
