package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat;

public interface ParcellaD32Format extends ParcellaFormat {
  Spec SPEC = new Spec("parcella_d32", 0);

  @Override
  default Spec spec() {
    return SPEC;
  }
}
