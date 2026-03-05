package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelTransform;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcelformats.parcella.BlockPalette;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
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
        ServerLevelAccessor level,
        Vec3i originalSize,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable Config config) {
      super(level, originalSize, transform, dataDir, ignoreBlocks, ignoreEntities, config);
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

    try (var walk = Files.walk(ctx.subparcelsDir)) {
      walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(SUBPARCEL_SUFFIX))
          .forEach(
              subparcelPath -> {
                try {
                  loadSubparcel(ctx, subparcelPath, problemReporter);
                } catch (IOException e) {
                  LOGGER.error("Error loading subparcel: {}", subparcelPath, e);
                }
              });
    }
  }

  protected void loadSubparcel(Context ctx, Path subparcelFile, ProblemReporter problemReporter)
      throws IOException {

    // TODO
    throw new UnsupportedOperationException("Unimplemented yet");
    //
    //    BlockPos anchorPos = ctx.transform.apply(new BlockPos(ctx.config.anchorOffset));
    //
    //    String relativePath = ctx.subparcelsDir.relativize(subparcelFile).toString();
    //    String fileName = relativePath.substring(0, relativePath.length() - 4);
    //    String[] pathParts = fileName.split("/|");
    //
    //    long index = 0;
    //    for (int i = pathParts.length - 1; i >= 0; i--) {
    //      index = (index << 8) | Integer.parseInt(pathParts[i], 16);
    //    }
    //
    //    var coord = ZOrder3D.indexToCoordSigned(index);
    //    int gridSize = 16;
    //    int originX = anchorPos.getX() + coord.x * gridSize;
    //    int originY = anchorPos.getY() + coord.y * gridSize;
    //    int originZ = anchorPos.getZ() + coord.z * gridSize;
    //
    //    Subparcel subparcel = new Subparcel(originX, originY, originZ, gridSize, gridSize,
    // gridSize);

  }
}
