package io.github.leawind.gitparcel.parcelformats.parcella.d32;

import io.github.leawind.gitparcel.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelStorage;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.BlockPalette;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.ParcelUtils;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.utils.numbase.Base32Utils;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

public class ParcellaD32Saver
    implements ParcellaD32Format, ParcelFormat.Saver<ParcellaD32Format.Config> {

  public static final class Context extends SaveContext<Config> {
    public final Path blocksDir;
    public final Path blocksPaletteFile;
    public final Path entitiesDir;

    public BlockPalette blockPalette;

    public Context(
        Level level,
        Vec3i parcelSize,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        Config config) {
      super(level, parcelSize, transform, anchor, dataDir, ignoreEntities, config);
      blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      entitiesDir = dataDir.resolve(ENTITIES_DIR_NAME);
    }
  }

  /**
   * @param pos Local position
   */
  public record BlockEntityEntry(BlockPos pos, CompoundTag data) {
    /** Compare by (y, x, z) for deterministic ordering. */
    static final Comparator<BlockEntityEntry> COMPARATOR =
        Comparator.comparingInt(BlockEntityEntry::y)
            .thenComparingInt(BlockEntityEntry::x)
            .thenComparingInt(BlockEntityEntry::z);

    int x() {
      return pos.getX();
    }

    int y() {
      return pos.getY();
    }

    int z() {
      return pos.getZ();
    }
  }

  @Override
  public void save(
      Level level,
      Vec3i parcelSize,
      Vec3i anchor,
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreEntities,
      @Nullable Config config)
      throws IOException, ParcelException.UnsupportedFeature {
    if (config == null) {
      config = new Config();
    }

    var ctx = new Context(level, parcelSize, anchor, transform, dataDir, ignoreEntities, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(ParcelStorage.LOGGER)) {
      saveBlocks(ctx, 32);

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
    BlockPos anchorPos = new BlockPos(ctx.anchor);
    for (var localSubparcel : ParcelUtils.subdivideParcel(ctx.parcelSize, anchorPos, gridSize)) {
      Vec3i coord = localSubparcel.getCoord(gridSize, anchorPos);

      long index = ZOrder3D.coordToIndexSigned(coord);

      Path blockStateFile =
          subParcelsDir.resolve(RadixTreePathGenerator.toPath(index, SUBPARCEL_BLOCK_STATE_SUFFIX));
      Path blockEntityFile =
          subParcelsDir.resolve(
              RadixTreePathGenerator.toPath(index, SUBPARCEL_BLOCK_ENTITY_SUFFIX));
      Files.createDirectories(blockStateFile.getParent());

      List<BlockEntityEntry> blockEntities = new ArrayList<>();

      switch (ctx.config.subparcelFormat.get()) {
        case FLAT -> writeSubparcelFLAT(ctx, blockStateFile, localSubparcel, blockEntities);
        case RLE3D -> writeSubparcelRLE3D(ctx, blockStateFile, localSubparcel, blockEntities);
      }

      if (!blockEntities.isEmpty()) {
        writeBlockEntitySnbt(blockEntityFile, blockEntities);
      } else {
        Files.deleteIfExists(blockEntityFile);
      }
    }

    ctx.blockPalette.save(ctx.blocksPaletteFile);
    ctx.blockPalette = null;
  }

  /** Write block entity data as formatted SNBT for a subparcel. */
  protected void writeBlockEntitySnbt(Path blockEntityFile, List<BlockEntityEntry> blockEntities)
      throws IOException {
    // Sort by (y, x, z) for deterministic ordering
    blockEntities.sort(BlockEntityEntry.COMPARATOR);

    ListTag listTag = new ListTag();
    for (var entry : blockEntities) {
      CompoundTag entryTag = new CompoundTag();

      // Encode position as [x, y, z] using BlockPos.CODEC
      Tag posTag =
          BlockPos.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry.pos).getOrThrow();
      entryTag.put("pos", posTag);

      entryTag.put("data", entry.data);
      listTag.add(entryTag);
    }

    String snbt = ParcellaD32Format.formatSnbt(listTag);
    Files.writeString(blockEntityFile, snbt, StandardCharsets.UTF_8);
  }

  protected BlockPalette loadBlockPaletteIfExistElseCreate(Context ctx) {
    if (Files.exists(ctx.blocksPaletteFile)) {
      try {
        return BlockPalette.load(ctx.level, ctx.blocksPaletteFile);
      } catch (Exception e) {
        ParcelStorage.LOGGER.error("Error loading block palette: {}", e.getMessage(), e);
        return new BlockPalette();
      }
    } else {
      return new BlockPalette();
    }
  }

  protected void writeSubparcelRLE3D(
      Context ctx, Path file, Subparcel subparcel, List<BlockEntityEntry> blockEntities)
      throws IOException {
    var sb = new StringBuilder(8192);
    char[] base32Chars = Base32Utils.CHARS;

    var level = ctx.level;
    var palette = ctx.blockPalette;
    var transform = ctx.transform;

    var runs =
        VolumetricRLE.IMPL.encode(
            subparcel.sizeX,
            subparcel.sizeY,
            subparcel.sizeZ,
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
              if (blockEntity != null) {
                CompoundTag nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
                blockEntities.add(new BlockEntityEntry(new BlockPos(x, y, z), nbt));
              }

              return palette.collect(blockState);
            });

    for (var run : runs) {
      sb.append(base32Chars[run.minX()])
          .append(base32Chars[run.minY()])
          .append(base32Chars[run.minZ()]);
      int maxX = run.maxX();
      int maxY = run.maxY();
      int maxZ = run.maxZ();

      if (run.minX() != maxX || run.minY() != maxY || run.minZ() != maxZ) {
        sb.append(base32Chars[maxX]).append(base32Chars[maxY]).append(base32Chars[maxZ]);
      }

      // Palette ID use hex format
      sb.append('=').append(HexUtils.toHexUpperCase(run.value())).append('\n');
    }

    Files.writeString(file, sb, StandardCharsets.UTF_8);
  }

  protected void writeSubparcelFLAT(
      Context ctx, Path path, Subparcel subparcel, List<BlockEntityEntry> blockEntities)
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
          if (blockEntity != null) {
            CompoundTag nbt = blockEntity.saveWithFullMetadata(level.registryAccess());
            blockEntities.add(new BlockEntityEntry(new BlockPos(x, y, z), nbt));
          }

          int id = palette.collect(blockState);

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
      Path filePath = EntityNbtFilePath.resolve(ctx.entitiesDir, nbtFormat, entityId);
      nbtFormat.write(filePath, tag);

      entityId++;
    }
    removeRedundantEntityFiles(ctx.entitiesDir, nbtFormat, entityId);
  }

  protected void removeRedundantEntityFiles(Path entitiesDir, NbtFormat nbtFormat, int idThreshold)
      throws IOException {
    var files = entitiesDir.toFile().listFiles((dir, name) -> name.endsWith(nbtFormat.suffix));

    if (files == null) {
      return;
    }

    for (var file : files) {
      if (file.isFile()) {
        var path = file.toPath();
        if (EntityNbtFilePath.fromPath(path, nbtFormat) >= idThreshold) {
          ParcelStorage.LOGGER.info("Removing redundant entity file: {}", path);
          Files.delete(path);
        }
      }
    }
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
