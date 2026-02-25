package io.github.leawind.gitparcel.parcel.formats.structuretemplate;

import com.google.common.collect.ImmutableList;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureTemplateV0 implements ParcelFormat {
  @Override
  public String id() {
    return "structure_template";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends StructureTemplateV0 implements ParcelFormat.Save {
    @Override
    public void save(
        Level level, BlockPos from, Vec3i size, Path dir, boolean saveBlocks, boolean saveEntities)
        throws IOException {
      Files.createDirectories(dir);

      StructureTemplate template = new StructureTemplate();
      template.fillFromWorld(level, from, size, true, ImmutableList.of());

      CompoundTag tag = new CompoundTag();
      tag = template.save(tag);

      Path structureFile = dir.resolve("structure.nbt");
      try (OutputStream outputStream = Files.newOutputStream(structureFile)) {
        NbtIo.writeCompressed(tag, outputStream);
      }
    }
  }
}
