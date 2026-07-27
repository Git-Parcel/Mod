package io.github.leawind.gitparcel.common.minecraft.logic.builtin;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Writer;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Writer;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftCoreRecordProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.PaintingRecordProcessor;

/** Built-in formats and Minecraft semantic processors, discovered through the public SPI. */
@AutoService(GitParcelExtension.class)
public final class BuiltinExtension implements GitParcelExtension {
  @Override
  public String id() {
    return "gitparcel:builtin";
  }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    registrar.registerFormat(new ParcellaD32Writer());
    registrar.registerFormat(new ParcellaD32Reader());
    registrar.registerFormat(new ParcellaD16Writer());
    registrar.registerFormat(new ParcellaD16Reader());
    registrar.registerProcessor(new MinecraftCoreRecordProcessor());
    registrar.registerProcessor(new PaintingRecordProcessor());
  }
}
