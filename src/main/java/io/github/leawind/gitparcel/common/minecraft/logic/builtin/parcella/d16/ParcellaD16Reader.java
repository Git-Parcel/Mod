package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaDigitCodec;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaReader;

public final class ParcellaD16Reader extends ParcellaReader implements ParcellaD16Format {
  public ParcellaD16Reader() {
    super(16, ParcellaDigitCodec.HEX16);
  }
}
