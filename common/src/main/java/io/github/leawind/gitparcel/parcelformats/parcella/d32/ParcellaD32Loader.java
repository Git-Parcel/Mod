package io.github.leawind.gitparcel.parcelformats.parcella.d32;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcelformats.parcella.BlockPalette;
import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.SubparcelFormat;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.IndexPathCodec;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.utils.numbase.Base32Utils;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParcellaD32Loader
    implements ParcellaD32Format, ParcelFormat.Load<ParcellaD32Format.Config> {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final class Context extends LoadContext<Config> {
    public final Path blocksDir;
    public final Path blocksPaletteFile;
    public final Path blocksNbtDir;
    public final Path subparcelsDir;
    public final Path entitiesDir;

    public BlockPalette blockPalette;

    public Context(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable Config config) {
      super(level, parcelSize, transform, dataDir, ignoreBlocks, ignoreEntities, flags, config);
      blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      blocksNbtDir = blocksDir.resolve(NBT_DIR_NAME);
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
      ParcelTransform transform,
      Path dataDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Block.UpdateFlags int flags,
      @Nullable Config config)
      throws IOException, ParcelException {
    LOGGER.debug("Loading from: {}", dataDir);
    LOGGER.debug("    Size: {}", size);
    LOGGER.debug("    Transform: {}", transform);
    LOGGER.debug("    Ignore blocks: {}", ignoreBlocks);
    LOGGER.debug("    Ignore entities: {}", ignoreEntities);
    LOGGER.debug("    Update flags: {}", flags);
    LOGGER.debug("    Config: {}", config);

    Context ctx =
        new Context(level, size, transform, dataDir, ignoreBlocks, ignoreEntities, flags, config);

    try (var problemReporter = new ProblemReporter.ScopedCollector(LOGGER)) {
      if (!ignoreBlocks) {
        loadBlocks(ctx, problemReporter);
      }

      if (!ignoreEntities) {
        // TODO load entities
        // loadEntities(ctx, problemReporter);
      }
    }
  }

  protected void loadBlocks(Context ctx, ProblemReporter problemReporter)
      throws IOException, ParcelException {

    if (!Files.exists(ctx.blocksDir)) {
      // TODO error handling
      throw new ParcelException("Blocks directory not found: " + ctx.blocksDir);
    }

    loadSubparcels(ctx, 32, problemReporter);
  }

  protected void loadSubparcels(Context ctx, int gridSize, ProblemReporter problemReporter)
      throws IOException, ParcelException {

    if (!Files.exists(ctx.subparcelsDir)) {
      // TODO error handling
      throw new ParcelException("Subparcels directory not found: " + ctx.subparcelsDir);
    }

    ctx.blockPalette =
        BlockPalette.load(
            ctx.level,
            ctx.blocksPaletteFile,
            ctx.blocksNbtDir,
            ctx.config.blockEntityDataFormat.get());

    // Split the parcel into subparcels
    BlockPos anchorPos = new BlockPos(ctx.config.anchorOffset);
    for (var localSubparcel : Subparcel.subdivideParcel(gridSize, ctx.parcelSize, anchorPos)) {
      Vec3i coord = localSubparcel.getCoord(gridSize, anchorPos);
      long index = ZOrder3D.coordToIndexSigned(coord);
      Path subparcelFile =
          ctx.subparcelsDir.resolve(IndexPathCodec.indexToPath(index, SUBPARCEL_SUFFIX));

      if (Files.exists(subparcelFile)) {
        loadSubparcel(ctx, subparcelFile, localSubparcel, problemReporter);
      }
    }
  }

  protected void loadSubparcel(
      Context ctx, Path file, Subparcel localSubparcel, ProblemReporter problemReporter) {
    try {
      byte[] bytes = Files.readAllBytes(file);
      SubparcelFormat subparcelFormat = detectSubparcelFormat(bytes);
      int[][][] blockStates =
          switch (subparcelFormat) {
            case RLE3D -> loadSubparcelRLE3D(localSubparcel, bytes, problemReporter);
            case FLAT -> loadSubparcelFLAT(localSubparcel, bytes, problemReporter);
          };
      var localPos = new BlockPos.MutableBlockPos();

      for (int x = 0; x < localSubparcel.sizeX; x++) {
        for (int y = 0; y < localSubparcel.sizeY; y++) {
          for (int z = 0; z < localSubparcel.sizeZ; z++) {
            int paletteId = blockStates[x][y][z];
            BlockPalette.Data data = ctx.blockPalette.get(paletteId);
            if (data == null) {
              problemReporter.report(() -> String.format("Unknown block palette id %d", paletteId));
              continue;
            }
            BlockState blockState = data.blockState();
            BlockState worldBlockState = ctx.transform.apply(blockState);

            localPos.set(
                localSubparcel.originX + x, localSubparcel.originY + y, localSubparcel.originZ + z);
            BlockPos worldPos = ctx.transform.apply(localPos);

            ctx.level.setBlock(worldPos, worldBlockState, ctx.flags);
            var nbt = data.nbt();
            if (nbt != null) {
              BlockEntity blockEntity = ctx.level.getBlockEntity(worldPos);
              if (blockEntity != null) {
                blockEntity.loadWithComponents(
                    TagValueInput.create(problemReporter, ctx.level.registryAccess(), nbt));
              }
            }
          }
        }
      }

    } catch (IOException e) {
      problemReporter.report(() -> "Error reading subparcel file: " + file);
    }
  }

  protected int[][][] loadSubparcelRLE3D(
      Subparcel localSubparcel, byte[] bytes, ProblemReporter problemReporter) {

    int sizeX = localSubparcel.sizeX;
    int sizeY = localSubparcel.sizeY;
    int sizeZ = localSubparcel.sizeZ;

    int[][][] blockStates = new int[sizeX][sizeY][sizeZ];

    byte x0 = 0, y0 = 0, z0 = 0;
    byte x1 = 0, y1 = 0, z1 = 0;

    // Buffer to store the current line
    byte[] buff = new byte[16];
    byte len = 0;

    boolean skipThisLine = false;

    for_each_byte:
    for (byte b : bytes) {
      if (skipThisLine && b == '\n') {
        skipThisLine = false;
        continue;
      }

      to_report_invalid_line:
      do {
        switch (b) {
          case '=' -> {
            x0 = Base32Utils.parseChar(buff[0]);
            y0 = Base32Utils.parseChar(buff[1]);
            z0 = Base32Utils.parseChar(buff[2]);
            if (x0 == -1 || y0 == -1 || z0 == -1) {
              break to_report_invalid_line;
            }
            if (len == 3) {
              x1 = x0;
              y1 = y0;
              z1 = z0;
            } else {
              if (len != 6) {
                break to_report_invalid_line;
              }
              x1 = Base32Utils.parseChar(buff[3]);
              y1 = Base32Utils.parseChar(buff[4]);
              z1 = Base32Utils.parseChar(buff[5]);
              if (x1 == -1 || y1 == -1 || z1 == -1) {
                break to_report_invalid_line;
              }
            }

            len = 0;
          }
          case '\n' -> {
            int paletteId = HexUtils.parsePositive(buff, 0, len);
            if (paletteId == -1) {
              break to_report_invalid_line;
            }
            len = 0;

            for (int x = x0; x <= x1; x++) {
              for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                  blockStates[x][y][z] = paletteId;
                }
              }
            }
          }
          case '\r', ' ', '\t', '\0' -> {}
          default -> {
            if (len >= buff.length) {
              break to_report_invalid_line;
            }
            buff[len++] = b;
          }
        }
        continue for_each_byte;
      } while (false);

      // Report invalid line and skip the rest of the line
      skipThisLine = true;
      byte finalLen = len;
      problemReporter.report(
          () -> String.format("Invalid line: %s", new String(buff, 0, finalLen)));
    }

    return blockStates;
  }

  protected int[][][] loadSubparcelFLAT(
      Subparcel localSubparcel, byte[] bytes, ProblemReporter problemReporter) {
    int[][][] states = new int[localSubparcel.sizeX][localSubparcel.sizeY][localSubparcel.sizeZ];

    byte[] buff = new byte[8];
    byte len = 0;

    int blockIndex = 0;
    for (byte b : bytes) {
      switch (b) {
        case '\n' -> {
          int x = blockIndex % localSubparcel.sizeX;
          int y = (blockIndex / localSubparcel.sizeX) % localSubparcel.sizeY;
          int z = blockIndex / (localSubparcel.sizeX * localSubparcel.sizeY);

          try {
            states[x][y][z] = HexUtils.parsePositive(buff, 0, len);
          } catch (NumberFormatException e) {
            byte finalLen = len;
            problemReporter.report(
                () ->
                    String.format(
                        "Error parsing palette id '%s' at (%d, %d, %d)",
                        new String(buff, 0, finalLen), x, y, z));
          }

          len = 0;
          blockIndex++;
        }
        case '\r', ' ', '\t', '\0' -> {}
        default -> buff[len++] = b;
      }
    }
    return states;
  }

  /**
   * Detect the subparcel format from the first 7 bytes of the file
   *
   * <p>Note: If the file is invalid, the returned value is undefined.
   *
   * <p>This method never throws exception.
   *
   * @param bytes The content of the subparcel file
   * @return The detected subparcel format
   */
  protected static SubparcelFormat detectSubparcelFormat(byte[] bytes) {
    do {
      if (bytes.length < 8) {
        break;
      }
      if (bytes[3] == '=' || bytes[6] == '=') {
        return SubparcelFormat.RLE3D;
      }
    } while (false);

    return SubparcelFormat.FLAT;
  }
}
