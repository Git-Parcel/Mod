package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaDigitCodec;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaWriter;

public final class ParcellaD16Writer extends ParcellaWriter implements ParcellaD16Format {
  public ParcellaD16Writer() {
    super(16, ParcellaDigitCodec.HEX16);
  }
}
