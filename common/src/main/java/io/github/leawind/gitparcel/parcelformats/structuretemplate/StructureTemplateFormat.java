package io.github.leawind.gitparcel.parcelformats.structuretemplate;

import com.google.common.collect.ImmutableList;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtAccounterException;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

public class StructureTemplateFormat
    implements ParcelFormat.Save<ParcelFormatConfig.None>,
        ParcelFormat.Load<ParcelFormatConfig.None> {
  String NBT_FILE_NAME = "structure.nbt";

  private static final Info INFO = new Info("structure_template", 0);

  @Override
  public Info info() {
    return INFO;
  }

  @Override
  public void save(
      Level level,
      Vec3i parcelSize,
      Vec3i anchor,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreEntities,
      ParcelFormatConfig.@Nullable None config)
      throws IOException {
    if (transform.hasOrientation()) {
      throw new UnsupportedOperationException(
          "Mirror or rotation transform is not supported by this format");
    }

    StructureTemplate template = new StructureTemplate();
    template.fillFromWorld(
        level, transform.getTranslatedOrigin(), parcelSize, true, ImmutableList.of());
    CompoundTag tag = template.save(new CompoundTag());

    Files.createDirectories(dataDir);
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
      ServerLevelAccessor level,
      Vec3i size,
      Vec3i anchor,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      ParcelFormatConfig.@Nullable None config)
      throws IOException, ParcelException.CorruptedParcelException {
    LOGGER.info("Loading structure template with size {} and transform {}", size, transform);

    Path structureFile = dataDir.resolve(NBT_FILE_NAME);

    CompoundTag tag;
    try {
      // NbtAccounterException will be thrown when the NBT file is too large
      tag = NbtIo.readCompressed(structureFile, NbtAccounter.unlimitedHeap());
    } catch (NbtAccounterException e) {
      throw new ParcelException.CorruptedParcelException("The NBT file is too large", e);
    }
    var server = level.getServer();
    if (server == null) {
      throw new RuntimeException("Failed to get MinecraftServer from level " + level);
    }
    StructureTemplate template = server.getStructureManager().readStructure(tag);

    boolean isStrict = true;
    BlockPos pivotPos = transform.getTranslatedOrigin();

    StructurePlaceSettings settings =
        new StructurePlaceSettings()
            .setIgnoreEntities(!ignoreEntities)
            .setKnownShape(isStrict)
            .setMirror(transform.mirror())
            .setRotation(transform.rotation());

    template.placeInWorld(
        level, pivotPos, pivotPos, settings, RandomSource.create(pivotPos.asLong()), flags);
  }
}
