package io.github.leawind.gitparcel.common.minecraft.logic.builtin;

import com.google.auto.service.AutoService;
import io.github.leawind.gitparcel.common.api.extension.GitParcelExtension;
import io.github.leawind.gitparcel.common.api.extension.ParcelExtensionRegistrar;
import io.github.leawind.gitparcel.common.impl.content.AttachmentContentType;
import io.github.leawind.gitparcel.common.impl.content.BlockContentType;
import io.github.leawind.gitparcel.common.impl.content.EntityContentType;
import io.github.leawind.gitparcel.common.impl.content.ScheduledTickContentType;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.DeclaredCoordinateFieldProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.LodestoneCompassProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MapDataAttachmentType;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MapItemProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.MinecraftCoreRecordProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.PaintingRecordProcessor;
import io.github.leawind.gitparcel.common.minecraft.logic.portable.TransientFieldProcessor;

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
    registrar.registerContentType(new ScheduledTickContentType());
    registrar.registerProcessor(new MinecraftCoreRecordProcessor());
    registrar.registerProcessor(new PaintingRecordProcessor());
    registrar.registerProcessor(new DeclaredCoordinateFieldProcessor());
    registrar.registerProcessor(new MapItemProcessor());
    registrar.registerProcessor(new LodestoneCompassProcessor());
    registrar.registerProcessor(new TransientFieldProcessor());
    registrar.registerAttachmentType(MapDataAttachmentType.INSTANCE);
    VanillaFieldDeclarations.register(registrar);
  }
}
