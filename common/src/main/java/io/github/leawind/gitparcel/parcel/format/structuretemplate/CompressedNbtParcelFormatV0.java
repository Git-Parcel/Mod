package io.github.leawind.gitparcel.parcel.format.structuretemplate;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Like vanilla structure block */
public class CompressedNbtParcelFormatV0 extends ParcelFormat {
  public static ParcelFormat INSTANCE = new CompressedNbtParcelFormatV0();

  private CompressedNbtParcelFormatV0() {
    super("compressed_nbt", 0);
  }

  @Override
  public void saveContent(Parcel parcel, Path dir) throws IOException {
    Files.createDirectories(dir);

    StructureTemplate template = getStructureTemplate(parcel);

    CompoundTag tag = new CompoundTag();
    template.save(tag);

    Path structureFile = dir.resolve("structure.nbt");
    try (OutputStream outputStream = Files.newOutputStream(structureFile)) {
      NbtIo.writeCompressed(tag, outputStream);
    }
  }

  @Override
  public void loadContent(Parcel parcel, Path dir) throws IOException {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static StructureTemplate getStructureTemplate(Parcel parcel) {
    ServerLevel level = parcel.getLevel();
    StructureTemplate template = new StructureTemplate();
    template.fillFromWorld(
        level, parcel.getFromCorner(), parcel.getSize(), true, ImmutableList.of());
    return template;
  }
}
