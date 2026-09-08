package io.github.leawind.gitparcel.common.minecraft.logic.builtin;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.impl.content.AttachmentContentType;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.impl.content.EntityContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.DeclaredCoordinateFieldProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftCoreRecordProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.PaintingRecordProcessor;
import net.minecraft.resources.Identifier;

/** Built-in parcel content types and semantic processors, discovered through the public SPI. */
@AutoService(GitParcelExtension.class)
public final class BuiltinExtension implements GitParcelExtension {
  @Override
  public String id() {
    return "gitparcel:builtin";
  }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    registrar.registerContentType(new AttachmentContentType());
    registrar.registerContentType(new BlockContentType());
    registrar.registerContentType(new EntityContentType());
    registrar.registerProcessor(new MinecraftCoreRecordProcessor());
    registrar.registerProcessor(new PaintingRecordProcessor());
    registrar.registerProcessor(new DeclaredCoordinateFieldProcessor());
    registerVanillaCoordinateFields(registrar);
  }

  private static void registerVanillaCoordinateFields(ParcelExtensionRegistrar registrar) {
    for (String hive : new String[] {"beehive", "bee_nest"}) {
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(
              ParcelCoordinateField.Target.BLOCK_ENTITY,
              Identifier.fromNamespaceAndPath("minecraft", hive),
              "flower_pos",
              ParcelCoordinateField.Encoding.BLOCK_POS));
    }
  }
}
