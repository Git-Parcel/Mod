package io.github.leawind.gitparcel.parcel.formats.structuretemplate;

import com.google.common.collect.ImmutableList;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.config.ParcelFormatConfig;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtAccounterException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Note: The loader of this format always load blocks regardless of the value of `loadBlocks` */
public abstract class StructureTemplateFormatV0 implements ParcelFormat<ParcelFormatConfig.None> {
  public static final String NBT_FILE_NAME = "structure.nbt";

  @Override
  public String id() {
    return "structure_template";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends StructureTemplateFormatV0
      implements ParcelFormat.Save<ParcelFormatConfig.None> {
    @Override
    public void save(
        Level level,
        Parcel parcel,
        Path dataDir,
        boolean saveEntities,
        ParcelFormatConfig.None config)
        throws IOException {
      Files.createDirectories(dataDir);

      StructureTemplate template = new StructureTemplate();
      template.fillFromWorld(level, parcel.getOrigin(), parcel.getSize(), true, ImmutableList.of());
      CompoundTag tag = template.save(new CompoundTag());

      Path structureFile = dataDir.resolve(NBT_FILE_NAME);
      try (OutputStream outputStream = Files.newOutputStream(structureFile)) {
        NbtIo.writeCompressed(tag, outputStream);
      }
    }
  }

  public static final class Load extends StructureTemplateFormatV0
      implements ParcelFormat.Load<ParcelFormatConfig.None> {

    /**
     * @param loadBlocks This parameter is ignored, it always loads blocks
     */
    @Override
    public void load(
        ServerLevel level,
        BlockPos parcelOrigin,
        Path dataDir,
        boolean loadBlocks,
        boolean loadEntities)
        throws IOException, ParcelException {

      Path structureFile = dataDir.resolve(NBT_FILE_NAME);

      CompoundTag tag;
      try {
        // NbtAccounterException will be thrown when the NBT file is too large
        tag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());
      } catch (NbtAccounterException e) {
        throw new ParcelException.InvalidParcel("The NBT file is too large", e);
      }

      StructureTemplate template = level.getServer().getStructureManager().readStructure(tag);

      boolean isStrict = true;

      StructurePlaceSettings settings =
          new StructurePlaceSettings().setIgnoreEntities(!loadEntities).setKnownShape(isStrict);

      template.placeInWorld(
          level,
          parcelOrigin,
          parcelOrigin,
          settings,
          RandomSource.create(parcelOrigin.asLong()),
          2 | (isStrict ? 816 : 0));
    }
  }
}
