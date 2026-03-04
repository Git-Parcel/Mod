package io.github.leawind.gitparcel.parcelformats.structuretemplate;

import com.google.common.collect.ImmutableList;
import io.github.leawind.gitparcel.api.parcel.Parcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public class StructureTemplateFormat
    implements ParcelFormat.Save<ParcelFormatConfig.None>,
        ParcelFormat.Load<ParcelFormatConfig.None> {
  String NBT_FILE_NAME = "structure.nbt";

  @Override
  public String id() {
    return "structure_template";
  }

  @Override
  public int version() {
    return 0;
  }

  @Override
  public void save(
      Level level,
      Parcel parcel,
      Path dataDir,
      boolean ignoreEntities,
      ParcelFormatConfig.@Nullable None config)
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

  /**
   * @param ignoreBlocks This parameter is ignored, it always loads blocks
   */
  @Override
  public void load(
      ServerLevel level,
      Parcel parcel,
      Path dataDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      ParcelFormatConfig.@Nullable None config)
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
        new StructurePlaceSettings().setIgnoreEntities(!ignoreEntities).setKnownShape(isStrict);

    BlockPos parcelOrigin = parcel.getOrigin();

    template.placeInWorld(
        level,
        parcelOrigin,
        parcelOrigin,
        settings,
        RandomSource.create(parcelOrigin.asLong()),
        flags);
  }
}
