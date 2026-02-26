package io.github.leawind.gitparcel.parcel.formats.structuretemplate;

import com.google.common.collect.ImmutableList;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Note: The loader of this format always load blocks regardless of the value of `loadBlocks` */
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
    public void save(Level level, BlockPos from, Vec3i size, Path dir, boolean saveEntities)
        throws IOException {
      Files.createDirectories(dir);

      StructureTemplate template = new StructureTemplate();
      template.fillFromWorld(level, from, size, true, ImmutableList.of());
      CompoundTag tag = template.save(new CompoundTag());

      Path structureFile = dir.resolve("structure.nbt");
      try (OutputStream outputStream = Files.newOutputStream(structureFile)) {
        NbtIo.writeCompressed(tag, outputStream);
      }
    }
  }

  public static final class Load extends StructureTemplateV0 implements ParcelFormat.Load {

    /**
     * @param loadBlocks This parameter is ignored, it always loads blocks
     */
    @Override
    public void load(
        ServerLevel level, BlockPos pos, Path dir, boolean loadBlocks, boolean loadEntities)
        throws IOException, Parcel.ParcelException {
      Path structureFile = dir.resolve("structure.nbt");
      CompoundTag tag = NbtIo.readCompressed(structureFile, NbtAccounter.uncompressedQuota());
      StructureTemplate template = level.getServer().getStructureManager().readStructure(tag);
      StructurePlaceSettings settings = new StructurePlaceSettings();
      settings.setIgnoreEntities(!loadEntities);
      template.placeInWorld(level, pos, pos, settings, RandomSource.create(pos.asLong()), 816);
    }
  }
}
