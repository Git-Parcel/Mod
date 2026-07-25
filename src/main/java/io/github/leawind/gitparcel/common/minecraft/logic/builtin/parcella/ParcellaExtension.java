package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Loader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Saver;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Loader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Saver;

/** Built-in Parcella formats. */
@AutoService(GitParcelExtension.class)
public final class ParcellaExtension implements GitParcelExtension {
  @Override
  public String id() {
    return "gitparcel:parcella";
  }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    registrar.registerFormat(new ParcellaD32Saver());
    registrar.registerFormat(new ParcellaD32Loader());
    registrar.registerFormat(new ParcellaD16Saver());
    registrar.registerFormat(new ParcellaD16Loader());
  }
}
