package io.github.leawind.gitparcel.parcel.formats.parcella;

import com.google.gson.Gson;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class ParcellaV0 implements ParcelFormat {
  private static final Gson GSON = new Gson();

  public static final String BLOCKS_DIR_NAME = "blocks";
  public static final String ENTITIES_DIR_NAME = "entities";
  public static final String NBT_DIR_NAME = "nbt";
  public static final String PALETTE_FILE_NAME = "palette.txt";
  public static final String SUB_PARCELS_DIR_NAME = "subparcels";

  @Override
  public String id() {
    return "parcella";
  }

  @Override
  public int version() {
    return 0;
  }

  public static class BlockPalette {
    public final Map<Data, Integer> map = new HashMap<>();
    public final List<Data> list = new ArrayList<>();
    public final Set<Integer> blockEntities = new HashSet<>();

    private int nextPaletteId = 0;

    public final Path palettePath;
    public final Path nbtDir;

    public BlockPalette(Path palettePath, Path nbtDir) {
      this.palettePath = palettePath;
      this.nbtDir = nbtDir;
    }

    public int collect(Level level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      CompoundTag tag = null;
      if (blockEntity != null) {
        tag = blockEntity.saveWithFullMetadata(level.registryAccess());
      }
      return collect(blockState, tag);
    }

    public int collect(BlockState blockState, @Nullable CompoundTag nbt) {
      return collect(new Data(blockState, nbt));
    }

    public int collect(Data data) {
      if (!map.containsKey(data)) {
        map.put(data, nextPaletteId);
        list.add(data);
        if (data.nbt != null) {
          blockEntities.add(nextPaletteId);
        }
        nextPaletteId++;
        return nextPaletteId - 1;
      }
      return map.get(data);
    }

    public void clear() {
      map.clear();
      list.clear();
      nextPaletteId = 0;
    }

    public void tryLoad() {
      // TODO try load block palette
    }

    public void save(boolean useSnbt) throws IOException {
      Files.createDirectories(nbtDir);
      try (BufferedWriter writer = Files.newBufferedWriter(palettePath, StandardCharsets.UTF_8)) {
        for (int i = 0; i < list.size(); i++) {
          Data data = list.get(i);
          String blockStateString =
              BuiltInRegistries.BLOCK.wrapAsHolder(data.blockState.getBlock()).getRegisteredName();
          writer.write(Integer.toHexString(i) + "=" + blockStateString);
          writer.newLine();
        }
      }
      // Save NBTs
      for (int id : blockEntities) {
        Data data = list.get(id);
        if (data.nbt != null) {
          if (useSnbt) {
            // TODO format
            Files.writeString(nbtDir.resolve(id + ".snbt"), data.nbt.toString());
          } else {
            NbtIo.write(data.nbt, nbtDir.resolve(id + ".nbt"));
          }
          // TODO remove redundant NBT files
        }
      }
    }

    public record Data(BlockState blockState, @Nullable CompoundTag nbt) {}
  }

  public static final class Save extends ParcellaV0 implements ParcelFormat.Save {
    public static class Options {
      private static final String SCHEMA_URL =
          "https://git-parcel.github.io/schemas/ParcellaFormatOptions.json";

      public boolean enableSnbtForBlockEntities = true;
      public boolean enableMicroparcel = false;
      public boolean enableSnbtForEntities = true;
      public Vec3i anchorOffset = Vec3i.ZERO;

      public static @Nullable Options tryLoad(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
          return GSON.fromJson(reader, Options.class);
        } catch (IOException e) {
          return null;
        }
      }

      public static Options tryLoadOrDefault(Path path) {
        Options options = Options.tryLoad(path);
        return options != null ? options : new Options();
      }
    }

    @Override
    public void save(
        Level level, BlockPos parcelOrigin, Vec3i parcelSize, Path dataDir, boolean saveEntities)
        throws IOException {
      try (ProblemReporter.ScopedCollector problemReporter =
          new ProblemReporter.ScopedCollector(LOGGER)) {
        Files.createDirectories(dataDir);

        Path formatOptionsFile = dataDir.resolve("format-options.json");
        Options options = Options.tryLoadOrDefault(formatOptionsFile);

        saveBlocks(level, parcelOrigin, parcelSize, dataDir, options);

        if (saveEntities) {
          saveEntities(problemReporter, level, parcelOrigin, parcelSize, dataDir, options);
        }
      }
    }

    private void saveBlocks(
        Level level, BlockPos parcelOrigin, Vec3i parcelSize, Path dataDir, Options options)
        throws IOException {

      Path blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);

      Files.createDirectories(blocksDir);

      Path nbtDir = blocksDir.resolve(NBT_DIR_NAME);
      BlockPalette palette = new BlockPalette(blocksDir.resolve(PALETTE_FILE_NAME), nbtDir);
      palette.tryLoad();

      // Process sub-parcels with Z-Order encoding
      Path subParcelsDir = blocksDir.resolve(SUB_PARCELS_DIR_NAME);
      Files.createDirectories(subParcelsDir);

      BlockPos anchorPos = parcelOrigin.offset(options.anchorOffset);
      Iterable<BoundingBox> subparcels = subdivideParcel(parcelOrigin, parcelSize, anchorPos);

      for (var subparcel : subparcels) {
        long index =
            ZOrder3D.coordToIndexSigned(
                (subparcel.minX() - anchorPos.getX()) / 16,
                (subparcel.minY() - anchorPos.getY()) / 16,
                (subparcel.minZ() - anchorPos.getZ()) / 16);
        Path subParcelFile = indexToPath(subParcelsDir, index);
        Files.createDirectories(subParcelFile.getParent());

        // Write sub-parcel data
        try (BufferedWriter writer =
            Files.newBufferedWriter(subParcelFile, StandardCharsets.UTF_8)) {

          BlockPos subparcelFrom =
              new BlockPos(subparcel.minX(), subparcel.minY(), subparcel.minZ());
          BlockPos subparcelTo = new BlockPos(subparcel.maxX(), subparcel.maxY(), subparcel.maxZ());
          writeSubparcel(
              writer, level, subparcelFrom, subparcelTo, palette, options.enableMicroparcel);
        }
      }

      palette.save(options.enableSnbtForBlockEntities);
    }

    private void writeSubparcel(
        BufferedWriter writer,
        Level level,
        BlockPos from,
        BlockPos to,
        BlockPalette palette,
        boolean enableMicroparcel)
        throws IOException {
      if (!enableMicroparcel) {
        for (int x = from.getX(); x < to.getX(); x++) {
          for (int y = from.getY(); y < to.getY(); y++) {
            for (int z = from.getZ(); z < to.getZ(); z++) {
              int id = palette.collect(level, new BlockPos(x, y, z));
              writer.write(Integer.toHexString(id));
              writer.newLine();
            }
          }
        }
      } else {
        // TODO microparcel
        LOGGER.warn("Microparcel is not implemented yet");
      }
    }

    private void saveEntities(
        ProblemReporter problemReporter,
        Level level,
        BlockPos from,
        Vec3i size,
        Path dir,
        Options options)
        throws IOException {

      // TODO remove redundant entities

      Path entitiesDir = dir.resolve(ENTITIES_DIR_NAME);
      Files.createDirectories(entitiesDir);

      AABB area =
          new AABB(
              from.getX(),
              from.getY(),
              from.getZ(),
              from.getX() + size.getX(),
              from.getY() + size.getY(),
              from.getZ() + size.getZ());
      List<Entity> entities =
          level.getEntities((Entity) null, area, entity -> !(entity instanceof Player));

      int entityId = 0;
      for (Entity entity : entities) {
        CompoundTag tag = getEntityNbt(problemReporter, from, entity);
        if (options.enableSnbtForEntities) {
          Files.writeString(entitiesDir.resolve(entityId + ".snbt"), tag.toString());
        } else {
          NbtIo.write(tag, entitiesDir.resolve(entityId + ".nbt"));
        }

        entityId++;
      }
    }

    /**
     * Get the NBT tag of an entity, with position relative to the parcel origin.
     *
     * @param problemReporter Problem reporter, refer to {@link StructureTemplate#fillFromWorld }
     * @param from Start position of the parcel, used to calculate relative position
     * @param entity Entity to save
     * @return NBT tag of the entity
     */
    private CompoundTag getEntityNbt(
        ProblemReporter problemReporter, BlockPos from, Entity entity) {
      CompoundTag tag = new CompoundTag();
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

    /**
     * @param parcelOrigin Start position of the parcel
     * @param parcelSize Size of the parcel
     * @param anchorPos Absolute position of origin point
     * @return Bounding boxes of subparcels, use absolute coordinates
     */
    public static Iterable<BoundingBox> subdivideParcel(
        BlockPos parcelOrigin, Vec3i parcelSize, Vec3i anchorPos) {
      List<BoundingBox> subparcels = new ArrayList<>(1);

      BlockPos to = parcelOrigin.offset(parcelSize);
      List<Integer> xDivisions =
          subdivideParcel1D(parcelOrigin.getX(), parcelSize.getX(), anchorPos.getX());
      List<Integer> yDivisions =
          subdivideParcel1D(parcelOrigin.getY(), parcelSize.getY(), anchorPos.getY());
      List<Integer> zDivisions =
          subdivideParcel1D(parcelOrigin.getZ(), parcelSize.getZ(), anchorPos.getZ());

      for (int i = 0; i < xDivisions.size() - 1; i++) {
        int minX = Math.max(xDivisions.get(i), parcelOrigin.getX());
        int maxX = Math.min(xDivisions.get(i + 1), to.getX());
        if (minX >= maxX) continue;

        for (int j = 0; j < yDivisions.size() - 1; j++) {
          int minY = Math.max(yDivisions.get(j), parcelOrigin.getY());
          int maxY = Math.min(yDivisions.get(j + 1), to.getY());
          if (minY >= maxY) continue;

          for (int k = 0; k < zDivisions.size() - 1; k++) {
            int minZ = Math.max(zDivisions.get(k), parcelOrigin.getZ());
            int maxZ = Math.min(zDivisions.get(k + 1), to.getZ());
            if (minZ >= maxZ) continue;

            subparcels.add(new BoundingBox(minX, minY, minZ, maxX - 1, maxY - 1, maxZ - 1));
          }
        }
      }

      return subparcels;
    }

    /**
     * @param size Must be positive
     * @return Divisions of the parcel, including start and end positions. Use absolute coordinates
     */
    static List<Integer> subdivideParcel1D(int origin, int size, int anchorPos) {
      List<Integer> divisions = new ArrayList<>();

      int current = origin;
      divisions.add(current);
      current = ceilToGrid16(anchorPos, current);

      int endExclusive = origin + size;
      while (current < endExclusive) {
        divisions.add(current);
        current += 16;
      }

      divisions.add(endExclusive);

      return divisions;
    }

    static int floorToGrid16(int grid, int value) {
      return value - Math.floorMod(value - grid, 16);
    }

    static int ceilToGrid16(int grid, int value) {
      return floorToGrid16(grid, value) + 16;
    }

    static Path indexToPath(Path root, long index) {
      if (index == 0) {
        return root.resolve("00.txt");
      }

      Path result = root;
      long value = index;

      List<String> parts = new ArrayList<>();
      while (value != 0) {
        int b = (int) (value & 0xFF);
        parts.add(String.format("%02X", b));
        value >>>= 8;
      }

      int last = parts.size() - 1;
      for (int i = 0; i < last; i++) {
        result = result.resolve(parts.get(i));
      }

      return result.resolve(parts.get(last) + ".txt");
    }
  }
}
