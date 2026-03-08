package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import io.github.leawind.gitparcel.algorithms.SubdivideAlgo;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.BlockPalette;
import io.github.leawind.gitparcel.parcelformats.parcella.Microparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.IndexPathCodec;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ParcellaD16Saver
    implements ParcellaD16Format, ParcelFormat.Save<ParcellaD16Format.Config> {

  public static final class Context extends SaveContext<Config> {
    public final Path blocksDir;
    public final Path blocksPaletteFile;
    public final Path blocksNbtDir;
    public final Path entitiesDir;

    public BlockPalette blockPalette;

    public Context(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        Config config) {
      super(level, parcelSize, transform, dataDir, ignoreEntities, config);
      blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      blocksNbtDir = blocksDir.resolve(NBT_DIR_NAME);
      entitiesDir = dataDir.resolve(ENTITIES_DIR_NAME);
    }
  }

  @Override
  public void save(
      Level level,
      Vec3i parcelSize,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreEntities,
      @Nullable Config config)
      throws IOException {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcelSize, transform, dataDir, ignoreEntities, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      saveBlocks(ctx, 16);

      if (!ignoreEntities) {
        saveEntities(ctx, problemReporter);
      }
    }
  }

  /**
   * Save blocks in parcella format.
   *
   * @param gridSize Grid size of sub-parcels.
   * @param ctx Context
   * @throws IOException If an I/O error occurs
   */
  protected void saveBlocks(Context ctx, int gridSize) throws IOException {

    Files.createDirectories(ctx.blocksDir);

    // Load or create block palette
    ctx.blockPalette = loadBlockPaletteIfExistElseCreate(ctx);

    Path subParcelsDir = ctx.blocksDir.resolve(SUBPARCELS_DIR_NAME);
    Files.createDirectories(subParcelsDir);

    // Split the parcel into subparcels
    BlockPos anchorPos = new BlockPos(ctx.config.anchorOffset);
    for (var localSubparcel : Subparcel.subdivideParcel(gridSize, ctx.parcelSize, anchorPos)) {
      Vec3i coord = localSubparcel.getCoord(gridSize, anchorPos);

      long index = ZOrder3D.coordToIndexSigned(coord);

      Path subparcelFile =
          subParcelsDir.resolve(IndexPathCodec.indexToPath(index, SUBPARCEL_SUFFIX));
      Files.createDirectories(subparcelFile.getParent());
      if (ctx.config.enableMicroparcel.get()) {
        writeSubparcelRLE3D(ctx, subparcelFile, localSubparcel);
      } else {
        writeSubparcelFLAT(ctx, subparcelFile, localSubparcel);
      }
    }

    ctx.blockPalette.save(
        ctx.blocksPaletteFile, ctx.blocksNbtDir, ctx.config.blockEntityDataFormat.get());
    ctx.blockPalette = null;
  }

  protected BlockPalette loadBlockPaletteIfExistElseCreate(Context ctx) {
    if (Files.exists(ctx.blocksPaletteFile)) {
      try {
        return BlockPalette.load(
            ctx.level,
            ctx.blocksPaletteFile,
            ctx.blocksNbtDir,
            ctx.config.blockEntityDataFormat.get());
      } catch (Exception e) {
        LOGGER.error("Error loading block palette: {}", e.getMessage(), e);
        return new BlockPalette();
      }
    } else {
      return new BlockPalette();
    }
  }

  protected void writeSubparcelRLE3D(Context ctx, Path file, Subparcel subparcel)
      throws IOException {
    var sb = new StringBuilder(8192);
    char[] hex = HexUtils.UPPER_HEX_DIGITS;

    var level = ctx.level;
    var palette = ctx.blockPalette;
    var transform = ctx.transform;

    List<Microparcel> microparcels =
        SubdivideAlgo.INSTANCE.subdivide(
            ctx.parcelSize.getX(),
            ctx.parcelSize.getY(),
            ctx.parcelSize.getZ(),
            (x, y, z) -> {
              BlockPos pos =
                  new BlockPos(x + subparcel.originX, y + subparcel.originY, z + subparcel.originZ);
              pos = transform.apply(pos);
              // pos: world space

              BlockState blockState = level.getBlockState(pos);
              // blockState: world space
              blockState = transform.applyInverted(blockState);
              // blockState: local space
              BlockEntity blockEntity = level.getBlockEntity(pos);
              CompoundTag nbt = null;
              if (blockEntity != null) {
                nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
              }

              return palette.collect(blockState, nbt);
            },
            Microparcel::new);

    for (var microparcel : microparcels) {
      sb.append(hex[microparcel.originX])
          .append(hex[microparcel.originY])
          .append(hex[microparcel.originZ]);

      if (microparcel.sizeX != 1 || microparcel.sizeY != 1 || microparcel.sizeZ != 1) {
        sb.append(hex[microparcel.sizeX - 1])
            .append(hex[microparcel.sizeY - 1])
            .append(hex[microparcel.sizeZ - 1]);
      }

      sb.append('=').append(HexUtils.toHexUpperCase(microparcel.value)).append('\n');
    }

    Files.writeString(file, sb, StandardCharsets.UTF_8);
  }

  protected void writeSubparcelFLAT(Context ctx, Path path, Subparcel subparcel)
      throws IOException {
    StringBuilder sb = new StringBuilder(8192);

    int originX = subparcel.originX;
    int originY = subparcel.originY;
    int originZ = subparcel.originZ;
    int sizeX = subparcel.sizeX;
    int sizeY = subparcel.sizeY;
    int sizeZ = subparcel.sizeZ;

    var level = ctx.level;
    var palette = ctx.blockPalette;

    for (int i = 0, x = 0; i < sizeX; i++, x++) {
      for (int j = 0, y = 0; j < sizeY; j++, y++) {
        for (int k = 0, z = 0; k < sizeZ; k++, z++) {
          BlockPos pos = new BlockPos(x + originX, y + originY, z + originZ);
          pos = ctx.transform.apply(pos);

          BlockState blockState = level.getBlockState(pos);
          // blockState: world space
          blockState = ctx.transform.applyInverted(blockState);
          // blockState: local space
          BlockEntity blockEntity = level.getBlockEntity(pos);
          CompoundTag nbt = null;
          if (blockEntity != null) {
            nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
          }

          int id = palette.collect(blockState, nbt);

          sb.append(HexUtils.toHexUpperCase(id)).append('\n');
        }
      }
    }

    Files.writeString(path, sb, StandardCharsets.UTF_8);
  }

  protected void saveEntities(Context ctx, ProblemReporter problemReporter) throws IOException {
    Files.createDirectories(ctx.entitiesDir);
    var origin = ctx.transform.getTranslatedOrigin();
    var worldSize = ctx.transform.applyToSize(ctx.parcelSize);

    AABB aabb =
        new AABB(
            origin.getX(),
            origin.getY(),
            origin.getZ(),
            worldSize.getX(),
            worldSize.getY(),
            worldSize.getZ());

    List<Entity> entities =
        ctx.level.getEntities((Entity) null, aabb, entity -> !(entity instanceof Player));

    NbtFormat nbtFormat = ctx.config.entityDataFormat.get();

    int entityId = 0;
    for (Entity entity : entities) {
      CompoundTag tag = getEntityNbt(ctx, problemReporter, entity);
      Path path = ctx.entitiesDir.resolve(entityId + nbtFormat.suffix);
      nbtFormat.write(path, tag);

      entityId++;
    }
    // TODO remove redundant entities
  }

  /**
   * Get the NBT tag of an entity, with position relative to the parcel origin.
   *
   * @param problemReporter Problem reporter, refer to {@link StructureTemplate#fillFromWorld }
   * @param ctx Context containing parcel information
   * @param entity Entity to save
   * @return NBT tag of the entity
   */
  protected CompoundTag getEntityNbt(Context ctx, ProblemReporter problemReporter, Entity entity) {
    CompoundTag tag = new CompoundTag();

    Vec3 worldPos = entity.position();

    var output = TagValueOutput.createWithContext(problemReporter, entity.registryAccess());
    entity.save(output);

    {
      Vec3 pos = ctx.transform.applyInverted(worldPos);
      ListTag list = new ListTag();
      list.add(DoubleTag.valueOf(pos.x));
      list.add(DoubleTag.valueOf(pos.y));
      list.add(DoubleTag.valueOf(pos.z));
      tag.put("pos", list);
    }

    {
      BlockPos blockPos;

      if (entity instanceof Painting painting) {
        blockPos = painting.getPos();
      } else {
        blockPos = BlockPos.containing(worldPos);
      }

      blockPos = ctx.transform.applyInverted(blockPos);

      ListTag list = new ListTag();
      list.add(IntTag.valueOf(blockPos.getX()));
      list.add(IntTag.valueOf(blockPos.getY()));
      list.add(IntTag.valueOf(blockPos.getZ()));

      tag.put("blockPos", list);
    }

    tag.put("nbt", output.buildResult().copy());
    return tag;
  }
}
