package io.github.leawind.gitparcel.parcel.formats.parcella;

import com.google.gson.Gson;
import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.formats.NbtFormat;
import io.github.leawind.gitparcel.utils.hex.HexUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class ParcellaFormatV0 implements ParcelFormat {
  private static final Gson GSON = new Gson();

  public static final String BLOCKS_DIR_NAME = "blocks";
  public static final String ENTITIES_DIR_NAME = "entities";
  public static final String NBT_DIR_NAME = "nbt";
  public static final String PALETTE_FILE_NAME = "palette.txt";
  public static final String SUB_PARCELS_DIR_NAME = "subparcels";

  protected NbtFormat blockEntityDataFormat = NbtFormat.Text;
  protected NbtFormat entityDataFormat = NbtFormat.Text;
  protected boolean enableMicroparcel = true;

  @Override
  public String id() {
    return "parcella";
  }

  @Override
  public int version() {
    return 0;
  }

  public static final class Save extends ParcellaFormatV0 implements ParcelFormat.Save {

    public static class Options {
      private static final String SCHEMA_URL =
          "https://git-parcel.github.io/schemas/ParcellaFormatOptions.json";

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
    public void save(Level level, Parcel parcel, Path dataDir, boolean saveEntities)
        throws IOException {
      try (ProblemReporter.ScopedCollector problemReporter =
          new ProblemReporter.ScopedCollector(LOGGER)) {
        Files.createDirectories(dataDir);

        Path formatOptionsFile = dataDir.resolve("format-options.json");
        Options options = Options.tryLoadOrDefault(formatOptionsFile);

        saveBlocks(level, parcel, dataDir, options);

        if (saveEntities) {
          saveEntities(problemReporter, level, parcel, dataDir, options);
        }
      }
    }

    private void saveBlocks(Level level, Parcel parcel, Path dataDir, Options options)
        throws IOException {

      Path blocksDir = dataDir.resolve(BLOCKS_DIR_NAME);
      Files.createDirectories(blocksDir);
      Path paletteFile = blocksDir.resolve(PALETTE_FILE_NAME);
      Path nbtDir = blocksDir.resolve(NBT_DIR_NAME);

      // Load or create block palette
      BlockPalette palette =
          loadBlockPaletteIfExistElseCreate(paletteFile, nbtDir, blockEntityDataFormat);

      // Process sub-parcels with Z-Order encoding
      Path subParcelsDir = blocksDir.resolve(SUB_PARCELS_DIR_NAME);
      Files.createDirectories(subParcelsDir);

      BlockPos anchorPos = parcel.getOrigin().offset(options.anchorOffset);
      Iterable<Subparcel> subparcels = Subparcel.subdivideParcel(parcel, anchorPos);

      for (var subparcel : subparcels) {
        Vec3i coord = subparcel.getCoord(anchorPos);
        long index = ZOrder3D.coordToIndexSigned(coord);

        Path subparcelRelativePath = IndexPathCodec.indexToPath(index, ".txt");
        Path subparcelFile = subParcelsDir.resolve(subparcelRelativePath);
        Files.createDirectories(subparcelFile.getParent());

        // Write sub-parcel data
        try (BufferedWriter writer =
            Files.newBufferedWriter(subparcelFile, StandardCharsets.UTF_8)) {
          writeSubparcel(writer, level, subparcel, palette, enableMicroparcel);
        }
      }

      palette.save(paletteFile, nbtDir, blockEntityDataFormat);
    }

    private void writeSubparcel(
        Path paletteFile, Path nbtDir, NbtFormat blockEntityDataFormat) {
      try {
        return BlockPalette.loadIfExist(paletteFile, nbtDir, blockEntityDataFormat);
      } catch (Exception e) {
        LOGGER.error("Error loading block palette: {}", e.getMessage(), e);
        return new BlockPalette();
      }
    }

        BufferedWriter writer,
        Level level,
        Subparcel subparcel,
        BlockPalette palette,
        boolean enableMicroparcel)
        throws IOException {
      StringBuilder sb = new StringBuilder(8192);

      if (enableMicroparcel) {
        char[] hex = HexUtils.UPPER_HEX_DIGITS;

        for (var microparcel : Microparcel.subdivide(subparcel, level, palette)) {
          sb.append(hex[microparcel.originX])
              .append(hex[microparcel.originY])
              .append(hex[microparcel.originZ]);

          if (microparcel.sizeX != 0 || microparcel.sizeY != 0 || microparcel.sizeZ != 0) {
            sb.append(hex[microparcel.sizeX])
                .append(hex[microparcel.sizeY])
                .append(hex[microparcel.sizeZ]);
          }

          sb.append('=').append(HexUtils.toHexUpperCase(microparcel.value)).append('\n');

          if (sb.length() > 8000) {
            writer.write(sb.toString());
            sb.setLength(0);
          }
        }
      } else {
        int originX = subparcel.originX;
        int originY = subparcel.originY;
        int originZ = subparcel.originZ;
        int sizeX = subparcel.sizeX;
        int sizeY = subparcel.sizeY;
        int sizeZ = subparcel.sizeZ;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0, x = originX; i < sizeX; i++, x++) {
          for (int j = 0, y = originY; j < sizeY; j++, y++) {
            for (int k = 0, z = originZ; k < sizeZ; k++, z++) {
              int id = palette.collect(level, pos.set(x, y, z));

              sb.append(HexUtils.toHexUpperCase(id)).append('\n');

              if (sb.length() > 8000) {
                writer.write(sb.toString());
                sb.setLength(0);
              }
            }
          }
        }
      }

      if (!sb.isEmpty()) {
        writer.write(sb.toString());
      }
    }

    private void saveEntities(
        ProblemReporter problemReporter, Level level, Parcel parcel, Path dir, Options options)
        throws IOException {

      // TODO remove redundant entities

      Path entitiesDir = dir.resolve(ENTITIES_DIR_NAME);
      Files.createDirectories(entitiesDir);

      List<Entity> entities =
          level.getEntities((Entity) null, parcel.getAABB(), entity -> !(entity instanceof Player));

      int entityId = 0;
      for (Entity entity : entities) {
        CompoundTag tag = getEntityNbt(problemReporter, parcel.getOrigin(), entity);
        Path path = entitiesDir.resolve(entityId + entityDataFormat.suffix);
        entityDataFormat.write(path, tag);

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
  }
}
