package io.github.leawind.gitparcel.parcel.formats.parcella.d16;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.ParcelFormatConfig;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcel.formats.NbtFormat;
import io.github.leawind.gitparcel.parcel.formats.parcella.BlockPalette;
import io.github.leawind.gitparcel.parcel.formats.parcella.Microparcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.utils.IndexPathCodec;
import io.github.leawind.gitparcel.parcel.formats.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.utils.config.BooleanConfigItem;
import io.github.leawind.gitparcel.utils.config.EnumConfigItem;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface ParcellaD16Format extends ParcelFormat.Impl<ParcellaD16Format.Config> {
  String BLOCKS_DIR_NAME = "blocks";
  String ENTITIES_DIR_NAME = "entities";
  String NBT_DIR_NAME = "nbt";
  String PALETTE_FILE_NAME = "palette.txt";
  String SUBPARCELS_DIR_NAME = "subparcels";
  String SUBPARCEL_SUFFIX = ".txt";

  @Override
  default String id() {
    return "parcella_d16";
  }

  @Override
  default int version() {
    return 0;
  }

  @Override
  default <T> Config castConfig(T config) throws ClassCastException {
    return (Config) config;
  }

  @Override
  default Config getDefaultConfig() {
    return new Config();
  }

  class Config extends ParcelFormatConfig<Config> {
    private static final String SCHEMA_URL =
        "https://git-parcel.github.io/schemas/ParcellaFormatConfig.json";

    public Vec3i anchorOffset = Vec3i.ZERO;

    public EnumConfigItem<NbtFormat> blockEntityDataFormat =
        new EnumConfigItem<>(NbtFormat.class, "blockEntityDataFormat")
            .defaultValue(NbtFormat.Text)
            .storeRightHere();
    public EnumConfigItem<NbtFormat> entityDataFormat =
        new EnumConfigItem<>(NbtFormat.class, "entityDataFormat")
            .defaultValue(NbtFormat.Text)
            .storeRightHere();
    public BooleanConfigItem enableMicroparcel =
        new BooleanConfigItem("enableMicroparcel").defaultValue(true).storeRightHere();

    public Config() {
      register(blockEntityDataFormat).register(entityDataFormat).register(enableMicroparcel);
    }
  }

  class Save implements ParcellaD16Format, ParcelFormat.Save<Config> {

    public static final class Context extends SaveContext<Config> {
      public final Path blocksDir;
      public final Path blocksPaletteFile;
      public final Path blocksNbtDir;
      public final Path entitiesDir;

      public BlockPalette blockPalette;

      public Context(
          Level level, Parcel parcel, Path dataDir, boolean saveEntities, Config config) {
        super(level, parcel, dataDir, saveEntities, config);
        blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
        blocksPaletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
        blocksNbtDir = blocksDir.resolve(NBT_DIR_NAME);
        entitiesDir = dataDir.resolve(ENTITIES_DIR_NAME);
      }
    }

    @Override
    public void save(
        Level level, Parcel parcel, Path dataDir, boolean saveEntities, @Nullable Config config)
        throws IOException {
      if (config == null) {
        config = new Config();
      }

      var ctx = new Context(level, parcel, dataDir, saveEntities, config);

      try (ProblemReporter.ScopedCollector problemReporter =
          new ProblemReporter.ScopedCollector(LOGGER)) {

        saveBlocks(ctx, 16);

        if (saveEntities) {
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

      // Process sub-parcels with Z-Order encoding
      Path subParcelsDir = ctx.blocksDir.resolve(SUBPARCELS_DIR_NAME);
      Files.createDirectories(subParcelsDir);

      BlockPos anchorPos = ctx.parcel.getOrigin().offset(ctx.config.anchorOffset);
      Iterable<Subparcel> subparcels = Subparcel.subdivideParcel(gridSize, ctx.parcel, anchorPos);

      for (var subparcel : subparcels) {
        Vec3i coord = subparcel.getCoord(gridSize, anchorPos);
        long index = ZOrder3D.coordToIndexSigned(coord);

        Path subparcelRelativePath = IndexPathCodec.indexToPath(index, SUBPARCEL_SUFFIX);
        Path subparcelFile = subParcelsDir.resolve(subparcelRelativePath);

        Files.createDirectories(subparcelFile.getParent());
        writeSubparcel(ctx, subparcelFile, subparcel);
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

    protected void writeSubparcel(Context ctx, Path file, Subparcel subparcel) throws IOException {
      if (ctx.config.enableMicroparcel.get()) {
        writeSubparcelWithMicroparcels(ctx, file, subparcel);
      } else {
        writeSubparcelFlat(ctx, file, subparcel);
      }
    }

    protected void writeSubparcelWithMicroparcels(Context ctx, Path file, Subparcel subparcel)
        throws IOException {
      var sb = new StringBuilder(8192);
      char[] hex = HexUtils.UPPER_HEX_DIGITS;

      for (var microparcel : Microparcel.subdivide(subparcel, ctx.level, ctx.blockPalette)) {
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

    protected void writeSubparcelFlat(Context ctx, Path path, Subparcel subparcel)
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

      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int i = 0, x = originX; i < sizeX; i++, x++) {
        for (int j = 0, y = originY; j < sizeY; j++, y++) {
          for (int k = 0, z = originZ; k < sizeZ; k++, z++) {

            int id = palette.collect(level, pos.set(x, y, z));

            sb.append(HexUtils.toHexUpperCase(id)).append('\n');
          }
        }
      }

      Files.writeString(path, sb, StandardCharsets.UTF_8);
    }

    protected void saveEntities(Context ctx, ProblemReporter problemReporter) throws IOException {

      // TODO remove redundant entities

      Path entitiesDir = ctx.dataDir.resolve(ENTITIES_DIR_NAME);
      Files.createDirectories(entitiesDir);

      List<Entity> entities =
          ctx.level.getEntities(
              (Entity) null, ctx.parcel.getAABB(), entity -> !(entity instanceof Player));

      int entityId = 0;
      for (Entity entity : entities) {
        CompoundTag tag = getEntityNbt(ctx, problemReporter, entity);
        Path path = entitiesDir.resolve(entityId + ctx.config.entityDataFormat.get().suffix);
        ctx.config.entityDataFormat.get().write(path, tag);

        entityId++;
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
    protected CompoundTag getEntityNbt(
        Context ctx, ProblemReporter problemReporter, Entity entity) {
      CompoundTag tag = new CompoundTag();
      BlockPos from = ctx.parcel.getOrigin();
      Vec3 pos =
          new Vec3(
              entity.getX() - (double) from.getX(),
              entity.getY() - (double) from.getY(),
              entity.getZ() - (double) from.getZ());

      BlockPos blockPos;
      if (entity instanceof Painting painting) {
        blockPos = painting.getPos().subtract(from);
      } else {
        blockPos = BlockPos.containing(pos);
      }

      TagValueOutput output =
          TagValueOutput.createWithContext(problemReporter, entity.registryAccess());
      entity.save(output);

      {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(pos.x));
        list.add(DoubleTag.valueOf(pos.y));
        list.add(DoubleTag.valueOf(pos.z));
        tag.put("pos", list);
      }
      {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(blockPos.getX()));
        list.add(IntTag.valueOf(blockPos.getY()));
        list.add(IntTag.valueOf(blockPos.getZ()));
        tag.put("blockPos", list);
      }
      {
        tag.put("nbt", output.buildResult().copy());
      }
      return tag;
    }
  }

  class Load implements ParcellaD16Format, ParcelFormat.Load<Config> {
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
          boolean loadBlocks,
          boolean loadEntities,
          @Nullable Config config) {
        super(level, parcel, dataDir, loadBlocks, loadEntities, config);
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
        boolean loadBlocks,
        boolean loadEntities,
        @Nullable Config config)
        throws IOException, ParcelException {
      LOGGER.debug("Loading from: {}", dataDir);
      LOGGER.debug("    Parcel: {}", parcel);
      LOGGER.debug("    Load blocks: {}", loadBlocks);
      LOGGER.debug("    Load entities: {}", loadEntities);
      LOGGER.debug("    Config: {}", config);

      Context ctx = new Context(level, parcel, dataDir, loadBlocks, loadEntities, config);

      try (ProblemReporter.ScopedCollector problemReporter =
          new ProblemReporter.ScopedCollector(LOGGER)) {
        if (loadBlocks) {
          loadBlocks(ctx, problemReporter);
        }

        if (loadEntities) {
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
}
