package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16;

import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat;

public interface ParcellaD16Format extends ParcellaFormat {

  Spec SPEC = new Spec("parcella_d16", 0);

  @Override
  default Spec spec() {
    return SPEC;
  }
}
