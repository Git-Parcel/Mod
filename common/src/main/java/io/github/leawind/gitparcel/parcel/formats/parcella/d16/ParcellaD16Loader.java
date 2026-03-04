package io.github.leawind.gitparcel.parcel.formats.parcella.d16;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcel.formats.parcella.BlockPalette;
import io.github.leawind.gitparcel.parcel.formats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.utils.ZOrder3D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ParcellaD16Loader
    implements ParcellaD16Format, ParcelFormat.Load<ParcellaD16Format.Config> {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final class Context extends LoadContext<Config> {
    public final Path blocksDir;
    public final Path blocksPaletteFile;
    public final Path blocksNbtDir;
    public final Path subparcelsDir;
    public final Path entitiesDir;

    public BlockPalette blockPalette;

    public Context(
        ServerLevel level,
        Parcel parcel,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Nullable Config config) {
      super(level, parcel, dataDir, ignoreBlocks, ignoreEntities, config);
      blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      blocksNbtDir = blocksDir.resolve(NBT_DIR_NAME);
      subparcelsDir = blocksDir.resolve(SUBPARCELS_DIR_NAME);
      entitiesDir = dataDir.resolve(ENTITIES_DIR_NAME);
    }

    public ServerLevel serverLevel() {
      return (ServerLevel) level;
    }
  }

  /**
   * @see StructureTemplate#placeInWorld
   */
  @Override
  public void load(
      ServerLevel level,
      Parcel parcel,
      Path dataDir,
      boolean ignoreBlocks,
      boolean ignoreEntities,
      @Nullable Config config)
      throws IOException, ParcelException {
    LOGGER.debug("Loading from: {}", dataDir);
    LOGGER.debug("    Parcel: {}", parcel);
    LOGGER.debug("    Ignore blocks: {}", ignoreBlocks);
    LOGGER.debug("    Ignore entities: {}", ignoreEntities);
    LOGGER.debug("    Config: {}", config);

    Context ctx = new Context(level, parcel, dataDir, ignoreBlocks, ignoreEntities, config);

    try (ProblemReporter.ScopedCollector problemReporter =
        new ProblemReporter.ScopedCollector(LOGGER)) {
      if (!ignoreBlocks) {
        loadBlocks(ctx, problemReporter);
      }

      if (!ignoreEntities) {
        // loadEntities(ctx, problemReporter);
      }
    }
  }

  protected void loadBlocks(Context ctx, ProblemReporter problemReporter)
      throws IOException, ParcelException {

    if (!Files.exists(ctx.blocksDir)) {
      LOGGER.warn("Blocks directory not found: {}", ctx.blocksDir);
      return;
    }

    ctx.blockPalette =
        BlockPalette.load(
            ctx.level,
            ctx.blocksPaletteFile,
            ctx.blocksNbtDir,
            ctx.config.blockEntityDataFormat.get());

    Path subParcelsDir = ctx.blocksDir.resolve(SUBPARCELS_DIR_NAME);
    if (Files.exists(subParcelsDir)) {
      loadSubparcels(ctx, problemReporter);
    }
  }

  protected void loadSubparcels(Context ctx, ProblemReporter problemReporter) throws IOException {
    BlockPos anchorPos = ctx.parcel.getOrigin().offset(ctx.config.anchorOffset);

    try (var walk = Files.walk(ctx.subparcelsDir)) {
      walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(SUBPARCEL_SUFFIX))
          .forEach(
              subparcelPath -> {
                try {
                  loadSubparcel(ctx, anchorPos, subparcelPath, problemReporter);
                } catch (IOException e) {
                  LOGGER.error("Error loading subparcel: {}", subparcelPath, e);
                }
              });
    }
  }

  protected void loadSubparcel(
      Context ctx, BlockPos anchorPos, Path subparcelFile, ProblemReporter problemReporter)
      throws IOException {
    String relativePath = ctx.subparcelsDir.relativize(subparcelFile).toString();
    String fileName = relativePath.substring(0, relativePath.length() - 4);
    String[] pathParts = fileName.split("/|");

    long index = 0;
    for (int i = pathParts.length - 1; i >= 0; i--) {
      index = (index << 8) | Integer.parseInt(pathParts[i], 16);
    }

    var coord = ZOrder3D.indexToCoordSigned(index);
    int gridSize = 16;
    int originX = anchorPos.getX() + coord.x * gridSize;
    int originY = anchorPos.getY() + coord.y * gridSize;
    int originZ = anchorPos.getZ() + coord.z * gridSize;

    Subparcel subparcel = new Subparcel(originX, originY, originZ, gridSize, gridSize, gridSize);

    // TODO
    throw new UnsupportedOperationException("Unimplemented yet");
  }
}
