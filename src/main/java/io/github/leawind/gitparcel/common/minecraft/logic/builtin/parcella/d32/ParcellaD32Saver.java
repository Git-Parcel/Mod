package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.NbtFormat;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.BlockPalette;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.Subparcel;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ParcellaUtils;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.common.utils.numbase.Base32Utils;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
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

    public @Nullable BlockPalette blockPalette = null;

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

    if (ctx.config.usePalette.get()) {
      ctx.blockPalette = loadBlockPaletteIfExistElseCreate(ctx);
    }

    Path subParcelsDir = ctx.blocksDir.resolve(SUBPARCELS_DIR_NAME);
    Files.createDirectories(subParcelsDir);

    // Split the parcel into subparcels
    BlockPos anchorPos = new BlockPos(ctx.anchor);
    for (var localSubparcel : ParcellaUtils.subdivideParcel(ctx.parcelSize, anchorPos, gridSize)) {
      Vec3i coord = localSubparcel.getCoord(gridSize, anchorPos);

      long index = ZOrder3D.coordToIndexSigned(coord);

      Path blockStateFile =
          RadixTreePathGenerator.toPath(subParcelsDir, index, SUBPARCEL_BLOCK_STATE_SUFFIX);
      Path blockEntityFile =
          RadixTreePathGenerator.toPath(subParcelsDir, index, SUBPARCEL_BLOCK_ENTITY_SUFFIX);
      Files.createDirectories(blockStateFile.getParent());

      BlockEntities blockEntities = new BlockEntities(new ArrayList<>());

      switch (ctx.config.subparcelFormat.get()) {
        case FLAT ->
            writeSubparcelFLAT(ctx, blockStateFile, localSubparcel, blockEntities.blockEntities());
        case RLE3D ->
            writeSubparcelRLE3D(ctx, blockStateFile, localSubparcel, blockEntities.blockEntities());
      }

      if (blockEntities.blockEntities().isEmpty()) {
        Files.deleteIfExists(blockEntityFile);
      } else {
        blockEntities.blockEntities().sort(BlockEntityEntry.COMPARATOR);

        CompoundTag tag =
            (CompoundTag)
                BlockEntities.CODEC.encodeStart(NbtOps.INSTANCE, blockEntities).getOrThrow();
        ctx.config.blockEntityDataFormat.get().write(blockEntityFile, tag);
      }
    }

    if (ctx.blockPalette != null) {
      ctx.blockPalette.save(ctx.blocksPaletteFile);
    }
  }

  protected BlockPalette loadBlockPaletteIfExistElseCreate(Context ctx) {
    if (Files.exists(ctx.blocksPaletteFile)) {
      try {
        return BlockPalette.load(ctx.blocksPaletteFile);
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
    var transform = ctx.transform;
    boolean usePalette = ctx.config.usePalette.get();
    var palette = ctx.blockPalette;

    // When no palette, use a temporary identity map for VolumetricRLE int IDs
    var stateToTempId = new IdentityHashMap<BlockState, Integer>();
    var tempIdToState = new ArrayList<BlockState>();

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

              if (usePalette) {
                return palette.collect(blockState);
              }
              return stateToTempId.computeIfAbsent(
                  blockState,
                  k -> {
                    int id = tempIdToState.size();
                    tempIdToState.add(k);
                    return id;
                  });
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

      sb.append(usePalette ? '~' : '=');
      if (usePalette) {
        sb.append(HexUtils.toHexUpperCase(run.value()));
      } else {
        sb.append(BlockPalette.stringifyBlockState(tempIdToState.get(run.value())));
      }
      sb.append('\n');
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
    boolean usePalette = ctx.config.usePalette.get();

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

          if (usePalette) {
            sb.append(HexUtils.toHexUpperCase(palette.collect(blockState)));
          } else {
            sb.append(BlockPalette.stringifyBlockState(blockState));
          }
          sb.append('\n');
        }
      }
    }

    Files.writeString(path, sb, StandardCharsets.UTF_8);
  }

  /**
   * @see ParcellaD32Loader#loadEntity
   */
  protected void saveEntities(Context ctx, ProblemReporter problemReporter) throws IOException {
    // Delete all existing entity files
    if (Files.exists(ctx.entitiesDir)) {
      try (var paths = Files.walk(ctx.entitiesDir)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
          Files.delete(path);
        }
      }
    }

    var origin = ctx.transform.getTranslatedOrigin();
    var worldSize = ctx.transform.applyToSize(ctx.parcelSize);

    AABB aabb =
        new AABB(
            origin.getX(),
            origin.getY(),
            origin.getZ(),
            origin.getX() + worldSize.getX(),
            origin.getY() + worldSize.getY(),
            origin.getZ() + worldSize.getZ());

    List<Entity> entities =
        ctx.level.getEntities((Entity) null, aabb, entity -> !(entity instanceof Player));

    NbtFormat nbtFormat = ctx.config.entityDataFormat.get();

    Map<Identifier, List<Entity>> byType = new LinkedHashMap<>();
    for (Entity entity : entities) {
      var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
      byType.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
    }

    for (Map.Entry<Identifier, List<Entity>> entry : byType.entrySet()) {
      var key = entry.getKey();
      Path dir = ctx.entitiesDir.resolve(key.getNamespace()).resolve(key.getPath());
      Files.createDirectories(dir);

      int index = 0;
      for (Entity entity : entry.getValue()) {
        nbtFormat.write(
            dir.resolve(index + nbtFormat.getSuffix()),
            getEntityNbt(ctx, problemReporter, entity),
            true);
        index++;
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
