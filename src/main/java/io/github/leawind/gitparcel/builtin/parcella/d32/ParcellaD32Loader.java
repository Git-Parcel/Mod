package io.github.leawind.gitparcel.builtin.parcella.d32;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.core.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.core.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.builtin.parcella.BlockPalette;
import io.github.leawind.gitparcel.builtin.parcella.Subparcel;
import io.github.leawind.gitparcel.builtin.parcella.SubparcelFormat;
import io.github.leawind.gitparcel.builtin.parcella.utils.ParcelUtils;
import io.github.leawind.gitparcel.builtin.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.builtin.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.core.util.numbase.Base32Utils;
import io.github.leawind.gitparcel.core.util.numbase.HexUtils;
import io.github.leawind.inventory.just.Result;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParcellaD32Loader
    implements ParcellaD32Format, ParcelFormat.Loader<ParcellaD32Format.Config> {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final class Context extends LoadContext<Config> {
    public final Path blocksDir;
    public final Path blocksPaletteFile;
    public final Path subparcelsDir;
    public final Path entitiesDir;

    public @Nullable BlockPalette blockPalette = null;

    public Context(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable Config config) {
      super(
          level,
          parcelSize,
          transform,
          anchor,
          dataDir,
          ignoreBlocks,
          ignoreEntities,
          flags,
          config);
      blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      subparcelsDir = blocksDir.resolve(SUBPARCELS_DIR_NAME);
      entitiesDir = dataDir.resolve(ENTITIES_DIR_NAME);
    }
  }

  /**
   * @see StructureTemplate#placeInWorld
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
      @Nullable Config config)
      throws IOException, ParcelException.CorruptedParcelException {
    LOGGER.debug("Loading from: {}", dataDir);
    LOGGER.debug("    Size: {}", size);
    LOGGER.debug("    Transform: {}", transform);
    LOGGER.debug("    Ignore blocks: {}", ignoreBlocks);
    LOGGER.debug("    Ignore entities: {}", ignoreEntities);
    LOGGER.debug("    Update flags: {}", flags);
    LOGGER.debug("    Config: {}", config);

    Context ctx =
        new Context(
            level, size, transform, anchor, dataDir, ignoreBlocks, ignoreEntities, flags, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      if (!ignoreBlocks) {
        loadBlocks(ctx, problemReporter);
      }

      if (!ignoreEntities) {
        loadEntities(ctx, problemReporter);
      }
    }
  }

  protected void loadBlocks(Context ctx, ProblemReporter problemReporter)
      throws IOException, ParcelException.CorruptedParcelException {

    if (!Files.exists(ctx.blocksDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Blocks directory not found: " + ctx.blocksDir);
    }

    loadSubparcels(ctx, 32, problemReporter);
  }

  protected void loadSubparcels(Context ctx, int gridSize, ProblemReporter problemReporter)
      throws IOException, ParcelException.CorruptedParcelException {

    if (!Files.exists(ctx.subparcelsDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Subparcels directory not found: " + ctx.subparcelsDir);
    }

    if (ctx.config.usePalette.get()) {
      ctx.blockPalette = BlockPalette.load(ctx.blocksPaletteFile);
    }

    // Split the parcel into subparcels
    BlockPos anchorPos = new BlockPos(ctx.anchor);
    for (var localSubparcel : ParcelUtils.subdivideParcel(ctx.parcelSize, anchorPos, gridSize)) {
      Vec3i coord = localSubparcel.getCoord(gridSize, anchorPos);
      long index = ZOrder3D.coordToIndexSigned(coord);
      Path blockStateFile =
          RadixTreePathGenerator.toPath(ctx.subparcelsDir, index, SUBPARCEL_BLOCK_STATE_SUFFIX);
      if (Files.exists(blockStateFile)) {
        // TODO if file not exist
        loadBlockStates(ctx, blockStateFile, localSubparcel, problemReporter);
      }

      Path blockEntityFile =
          RadixTreePathGenerator.toPath(ctx.subparcelsDir, index, SUBPARCEL_BLOCK_ENTITY_SUFFIX);
      if (Files.exists(blockEntityFile)) {
        loadBlockEntities(ctx, blockEntityFile, localSubparcel, problemReporter);
      }
    }
  }

  protected interface BlockStateLoader {
    void load(int localX, int localY, int localZ, BlockState localBlockState);
  }

  protected void loadBlockStates(
      Context ctx, Path blockStateFile, Subparcel localSubparcel, ProblemReporter problemReporter) {
    try {
      byte[] bytes = Files.readAllBytes(blockStateFile);
      SubparcelFormat subparcelFormat = detectSubparcelFormat(bytes);
      var localPos = new BlockPos.MutableBlockPos();

      BlockStateLoader blockStateLoader =
          (localX, localY, localZ, localBlockState) -> {
            BlockState worldBlockState = ctx.transform.apply(localBlockState);
            localPos.set(
                localSubparcel.originX + localX,
                localSubparcel.originY + localY,
                localSubparcel.originZ + localZ);
            BlockPos worldPos = ctx.transform.apply(localPos);

            ctx.level.setBlock(worldPos, worldBlockState, ctx.flags);
            ctx.level.getChunk(worldPos).removeBlockEntity(worldPos);
          };

      switch (subparcelFormat) {
        case RLE3D -> loadSubparcelBlockStatesRLE3D(ctx, bytes, blockStateLoader, problemReporter);
        case FLAT ->
            loadSubparcelBlockStatesFLAT(
                ctx, localSubparcel, bytes, blockStateLoader, problemReporter);
      }

    } catch (IOException e) {
      problemReporter.report(() -> "Error reading subparcel block state file: " + blockStateFile);
    }
  }

  protected void loadSubparcelBlockStatesRLE3D(
      Context ctx,
      byte[] bytes,
      BlockStateLoader blockStateLoader,
      ProblemReporter problemReporter) {

    byte x0 = 0, y0 = 0, z0 = 0;
    byte x1 = 0, y1 = 0, z1 = 0;

    // Buffer to store the current line content after separator
    byte[] buff = new byte[512];
    byte buffLen = 0;

    boolean skipThisLine = false;
    byte sepChar = 0; // 0 = not seen yet, '~' = palette ID, '=' = inline block state

    for_each_byte:
    for (byte b : bytes) {
      if (skipThisLine && b == '\n') {
        skipThisLine = false;
        continue;
      }

      to_report_invalid_line:
      do {

        switch (sepChar) {
          case 0 -> {
            switch (b) {
              case '~', '=' -> {

                // Parse coordinates from buff
                x0 = Base32Utils.parseChar(buff[0]);
                y0 = Base32Utils.parseChar(buff[1]);
                z0 = Base32Utils.parseChar(buff[2]);
                if (x0 == -1 || y0 == -1 || z0 == -1) {
                  break to_report_invalid_line;
                }

                if (buffLen == 3) {
                  x1 = x0;
                  y1 = y0;
                  z1 = z0;
                } else {
                  if (buffLen != 6) {
                    break to_report_invalid_line;
                  }
                  x1 = Base32Utils.parseChar(buff[3]);
                  y1 = Base32Utils.parseChar(buff[4]);
                  z1 = Base32Utils.parseChar(buff[5]);
                  if (x1 == -1 || y1 == -1 || z1 == -1) {
                    break to_report_invalid_line;
                  }
                }

                sepChar = b;
                buffLen = 0;
              }
              default -> buff[buffLen++] = b;
            }
          }
          case '~', '=' -> {
            if (b == '\n') {
              if (buffLen == 0) {
                break to_report_invalid_line;
              }

              switch (sepChar) {
                case '~' -> {
                  // <coordinate>~<palette_id>

                  if (ctx.blockPalette == null) {
                    byte finalLen = buffLen;
                    problemReporter.report(
                        () ->
                            String.format(
                                "Palette ID found ('~') but no palette is loaded. "
                                    + "Cannot resolve palette ID '%s'",
                                new String(buff, 0, finalLen)));
                    skipThisLine = true;
                    continue for_each_byte;
                  }

                  int paletteId = HexUtils.parsePositive(buff, 0, buffLen);
                  if (paletteId == -1) {
                    break to_report_invalid_line;
                  }
                  for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                      for (int z = z0; z <= z1; z++) {
                        BlockState blockState = ctx.blockPalette.get(paletteId);
                        if (blockState == null) {
                          problemReporter.report(
                              () -> String.format("Unknown block palette id %d", paletteId));
                          continue;
                        }
                        blockStateLoader.load(x, y, z, blockState);
                      }
                    }
                  }
                }
                case '=' -> {
                  // <coordinate>=<block_state>

                  String stateStr = new String(buff, 0, buffLen, StandardCharsets.UTF_8);

                  var parseResult = BlockPalette.parseBlockState(stateStr);
                  if (parseResult.isErr()) {
                    problemReporter.report(
                        () ->
                            String.format(
                                "Failed to parse block state '%s': %s",
                                stateStr, parseResult.unwrapErr()));
                    break to_report_invalid_line;
                  }
                  BlockState blockState = parseResult.unwrap();

                  for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                      for (int z = z0; z <= z1; z++) {
                        blockStateLoader.load(x, y, z, blockState);
                      }
                    }
                  }
                }
              }

              sepChar = 0;
              buffLen = 0;
            } else {
              buff[buffLen++] = b;
            }
          }
        }
        continue for_each_byte;
      } while (false);

      // Report invalid line and skip the rest of the line
      skipThisLine = true;
      byte finalLen = buffLen;
      sepChar = 0;
      buffLen = 0;
      problemReporter.report(
          () -> String.format("Invalid line: %s", new String(buff, 0, finalLen)));
    }
  }

  protected void loadSubparcelBlockStatesFLAT(
      Context ctx,
      Subparcel localSubparcel,
      byte[] bytes,
      BlockStateLoader blockStateLoader,
      ProblemReporter problemReporter) {
    int sizeX = localSubparcel.sizeX;
    int sizeY = localSubparcel.sizeY;
    int sizeZ = localSubparcel.sizeZ;

    byte[] buff = new byte[256];
    byte buffLen = 0;

    boolean usePalette = ctx.blockPalette != null;
    int blockIndex = 0;
    for (byte b : bytes) {
      switch (b) {
        case '\n' -> {
          int x = blockIndex / (sizeY * sizeZ);
          int y = (blockIndex / sizeZ) % sizeY;
          int z = blockIndex % sizeZ;

          if (usePalette) {
            var paletteId = HexUtils.parsePositive(buff, 0, buffLen);
            BlockState blockState = ctx.blockPalette.get(paletteId);
            if (blockState == null) {
              problemReporter.report(() -> String.format("Unknown block palette id %d", paletteId));
              continue;
            }
            blockStateLoader.load(x, y, z, blockState);
          } else {
            String stateStr = new String(buff, 0, buffLen, StandardCharsets.UTF_8);

            switch (BlockPalette.parseBlockState(stateStr)) {
              case Result.Ok(BlockState blockState) -> {
                blockStateLoader.load(x, y, z, blockState);
              }
              case Result.Err(String msg) ->
                  problemReporter.report(
                      () -> String.format("Failed to parse block state '%s': %s", stateStr, msg));
            }
          }

          buffLen = 0;
          blockIndex++;
        }
        case '\r', ' ', '\t', '\0' -> {}
        default -> buff[buffLen++] = b;
      }
    }
  }

  /** Note: If the file is invalid, the returned value is undefined. */
  protected static SubparcelFormat detectSubparcelFormat(byte[] bytes) {
    do {
      if (bytes.length < 8) {
        break;
      }
      if (bytes[3] == '=' || bytes[6] == '=' || bytes[3] == '~' || bytes[6] == '~') {
        return SubparcelFormat.RLE3D;
      }
    } while (false);

    return SubparcelFormat.FLAT;
  }

  protected void loadBlockEntities(
      Context ctx,
      Path blockEntityFile,
      Subparcel localSubparcel,
      ProblemReporter problemReporter) {

    BlockEntities blockEntities =
        switch (ctx.config.blockEntityDataFormat.get().read(blockEntityFile)) {
          case Result.Err(String err) -> {
            problemReporter.report(() -> "Invalid file: " + blockEntityFile + " " + err);
            yield null;
          }
          case Result.Ok(CompoundTag tag) -> {
            var parseResult = BlockEntities.CODEC.parse(NbtOps.INSTANCE, tag);
            if (parseResult.isError()) {
              problemReporter.report(() -> "Invalid block entities tag in " + blockEntityFile);
              yield null;
            }
            yield parseResult.getOrThrow();
          }
        };

    if (blockEntities == null) {
      return;
    }

    var list = blockEntities.blockEntities();

    for (var entry : list) {
      var localPos = entry.pos();
      int lx = localPos.getX();
      int ly = localPos.getY();
      int lz = localPos.getZ();

      // Check coordinates are within subparcel range
      if (lx < 0
          || lx >= localSubparcel.sizeX
          || ly < 0
          || ly >= localSubparcel.sizeY
          || lz < 0
          || lz >= localSubparcel.sizeZ) {
        problemReporter.report(
            () ->
                String.format(
                    "Block entity entry at (%d, %d, %d) is out of subparcel range in %s",
                    lx, ly, lz, blockEntityFile));
        continue;
      }

      var worldPos =
          ctx.transform.apply(
              new BlockPos(
                  localSubparcel.originX + lx,
                  localSubparcel.originY + ly,
                  localSubparcel.originZ + lz));

      var blockEntity = ctx.level.getBlockEntity(worldPos);
      if (blockEntity != null) {
        blockEntity.loadWithComponents(
            TagValueInput.create(problemReporter, ctx.level.registryAccess(), entry.data()));
      }
    }
  }

  protected void loadEntities(Context ctx, ProblemReporter problemReporter) throws IOException {
    if (!Files.exists(ctx.entitiesDir)) {
      return;
    }

    try (var stream = Files.list(ctx.entitiesDir)) {
      for (var namespaceDir : stream.toList()) {
        var entityNamespace = namespaceDir.getFileName().toString();
        try (var stream2 =
            Files.list(namespaceDir)
                .filter(path -> path.endsWith(ctx.config.entityDataFormat.get().suffix))) {
          for (var entityFile : stream2.toList()) {
            var entityKeyPath = entityFile.getFileName().toString();
            loadEntity(
                ctx,
                Identifier.fromNamespaceAndPath(entityNamespace, entityKeyPath),
                entityFile,
                problemReporter);
          }
        }
      }
    }
  }

  /**
   * @see ParcellaD32Saver#saveEntities
   */
  protected void loadEntity(
      Context ctx, Identifier identifier, Path path, ProblemReporter problemReporter) {
    try {
      var parseResult = ctx.config.entityDataFormat.get().read(path);
      if (parseResult.isErr()) {
        problemReporter.report(
            () -> "Failed to read entity file: " + path + " " + parseResult.unwrapErr());
        return;
      }

      CompoundTag wrapperTag = parseResult.unwrap();

      CompoundTag entityNbt =
          wrapperTag
              .getCompound("nbt")
              .orElseThrow(() -> new IllegalStateException("Missing 'nbt' field in entity data"));

      ListTag localPosList =
          wrapperTag
              .getList("pos")
              .orElseThrow(() -> new IllegalStateException("Missing 'pos' field in entity data"));

      Vec3 localPos =
          new Vec3(
              localPosList.getDouble(0).orElse(0.0),
              localPosList.getDouble(1).orElse(0.0),
              localPosList.getDouble(2).orElse(0.0));

      // Transform position from local space to world space
      Vec3 worldPos = ctx.transform.apply(localPos);

      // Override position in entity NBT
      ListTag worldPosList = new ListTag();
      worldPosList.add(DoubleTag.valueOf(worldPos.x));
      worldPosList.add(DoubleTag.valueOf(worldPos.y));
      worldPosList.add(DoubleTag.valueOf(worldPos.z));
      entityNbt.put("Pos", worldPosList);

      Entity entity =
          EntityType.loadEntityRecursive(
              entityNbt, ctx.level.getLevel(), EntitySpawnReason.LOAD, EntityProcessor.NOP);
      if (entity != null) {
        ctx.level.addFreshEntity(entity);
      }
    } catch (Exception e) {
      LOGGER.error("Error loading entity from {}: {}", path, e.getMessage(), e);
      problemReporter.report(() -> "Error loading entity from " + path + ": " + e.getMessage());
    }
  }
}
