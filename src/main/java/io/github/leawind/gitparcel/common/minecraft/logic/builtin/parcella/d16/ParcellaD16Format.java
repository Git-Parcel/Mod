package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Format;

public interface ParcellaD16Format extends ParcellaD32Format {

  Spec SPEC = new Spec("parcella_d16", 0);

  @Override
  default Spec spec() {
    return SPEC;
  }
}
