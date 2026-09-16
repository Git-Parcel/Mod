package io.github.leawind.gitparcel.common.minecraft.logic.builtin;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelCoordinateField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.impl.content.AttachmentContentType;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.impl.content.EntityContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.DeclaredCoordinateFieldProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MapDataAttachmentType;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MapItemProcessor;
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

  /** The builtin extension owns vanilla-field semantics in addition to its own namespace. */
  @Override
  public java.util.Collection<String> ownedNamespaces() {
    return java.util.Set.of("gitparcel", "minecraft");
  }

  @Override
  public void register(ParcelExtensionRegistrar registrar) {
    registrar.registerContentType(new AttachmentContentType());
    registrar.registerContentType(new BlockContentType());
    registrar.registerContentType(new EntityContentType());
    registrar.registerProcessor(new MinecraftCoreRecordProcessor());
    registrar.registerProcessor(new PaintingRecordProcessor());
    registrar.registerProcessor(new DeclaredCoordinateFieldProcessor());
    registrar.registerProcessor(new MapItemProcessor());
    registrar.registerAttachmentType(MapDataAttachmentType.INSTANCE);
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
    // The vanilla leash stores either a holder UUID or a fence position under "leash".
    registrar.registerCoordinateField(
        ParcelCoordinateField.forAny(
            ParcelCoordinateField.Target.ENTITY, "leash", ParcelCoordinateField.Encoding.BLOCK_POS));
    registrar.registerEntityRefField(ParcelEntityRefField.forAny("leash.UUID"));

    // Item frames carry a wall/floor facing plus an in-plane item rotation; shulkers carry a
    // hanging facing. All use the engine's 3D data value encoding.
    for (String frame : new String[] {"item_frame", "glow_item_frame"}) {
      var frameType = Identifier.fromNamespaceAndPath("minecraft", frame);
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(
              ParcelCoordinateField.Target.ENTITY,
              frameType,
              "Facing",
              ParcelCoordinateField.Encoding.DIRECTION));
      registrar.registerCoordinateField(
          ParcelCoordinateField.forType(
              ParcelCoordinateField.Target.ENTITY,
              frameType,
              "ItemRotation",
              ParcelCoordinateField.Encoding.ROTATION_STEP));
    }
    registrar.registerCoordinateField(
        ParcelCoordinateField.forType(
            ParcelCoordinateField.Target.ENTITY,
            Identifier.fromNamespaceAndPath("minecraft", "shulker"),
            "Facing",
            ParcelCoordinateField.Encoding.DIRECTION));
  }
}
