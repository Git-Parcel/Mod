package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaDigitCodec;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaWriter;

public final class ParcellaD32Writer extends ParcellaWriter implements ParcellaD32Format {
  public ParcellaD32Writer() {
    super(32, ParcellaDigitCodec.BASE32);
  }
}
