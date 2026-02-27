package io.github.leawind.gitparcel.parcel.formats.parcella;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.utils.hex.HexUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockPalette {
  private int nextId = 0;

  public final Map<Integer, Data> byId = new HashMap<>();
  public final Map<Data, Integer> byData = new HashMap<>();

  public final Set<Integer> blockEntities = new HashSet<>();

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
    String blockStateString =
        BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getRegisteredName();
    return collect(blockStateString, nbt);
  }

  public int collect(String blockStateString, @Nullable CompoundTag nbt) {
    return collect(new Data(blockStateString, nbt));
  }

  public int collect(Data data) {
    if (!byData.containsKey(data)) {
      int id = nextId++;

      byId.put(id, data);
      byData.put(data, id);

      if (data.nbt != null) {
        blockEntities.add(id);
      }

      return id;
    }
    return byData.get(data);
  }

  public void clear() {
    byData.clear();
    byId.clear();
    nextId = 0;
  }

  /**
   * Saves this block palette to the specified file and NBT directory.
   *
   * @param paletteFile the path to the palette file
   * @param nbtDir the directory to store NBT files
   * @param useSnbt whether to use SNBT format for NBT files. If false, NBT files will be saved in
   *     binary format.
   * @throws IOException if an I/O error occurs while saving the palette
   */
  public void save(Path paletteFile, Path nbtDir, boolean useSnbt) throws IOException {
    Files.createDirectories(nbtDir);
    try (BufferedWriter writer = Files.newBufferedWriter(paletteFile, StandardCharsets.UTF_8)) {
      for (var entry : byId.entrySet()) {
        int id = entry.getKey();
        Data data = entry.getValue();
        writer.write(HexUtils.toHexUpperCase(id) + "=" + data.blockStateString);
        writer.newLine();
      }
    }
    // Save NBTs
    for (int id : blockEntities) {
      Data data = byId.get(id);
      if (data.nbt != null) {
        if (useSnbt) {
          // TODO format
          Files.writeString(nbtDir.resolve(id + ".snbt"), data.nbt.toString());
        } else {
          NbtIo.write(data.nbt, nbtDir.resolve(id + ".nbt"));
        }
      }
    }
  }

  public record Data(String blockStateString, @Nullable CompoundTag nbt) {}

  /**
   * Loads a block palette from the specified file and NBT directory. If an error occurs, returns a
   * new palette.
   *
   * @param paletteFile the path to the palette file
   * @param nbtDir the directory to store NBT files
   * @param useSnbt whether to use SNBT format for NBT files. If false, NBT files will be saved in
   *     binary format.
   * @return the loaded block palette
   */
  public static BlockPalette loadOrNew(Path paletteFile, Path nbtDir, boolean useSnbt) {
    try {
      return load(paletteFile, nbtDir, useSnbt);
    } catch (IOException
        | InvalidPaletteException
        | NumberFormatException
        | CommandSyntaxException e) {
      ParcelFormat.LOGGER.error("Error loading block palette: {}", e.getMessage(), e);
      return new BlockPalette();
    }
  }

  /**
   * Loads a block palette from the specified file and NBT directory.
   *
   * @param paletteFile the path to the palette file
   * @param nbtDir the directory to store NBT files
   * @param useSnbt whether to use SNBT format for NBT files. If false, NBT files will be saved in
   *     binary format.
   * @return the loaded block palette
   * @throws IOException if an I/O error occurs
   * @throws InvalidPaletteException if the palette file is malformed
   * @throws NumberFormatException if an ID in the palette file is not a valid hexadecimal number
   * @throws CommandSyntaxException if the snbt format is used and the NBT file is malformed
   */
  public static BlockPalette load(Path paletteFile, Path nbtDir, boolean useSnbt)
      throws IOException, InvalidPaletteException, NumberFormatException, CommandSyntaxException {
    try (var reader = Files.newBufferedReader(paletteFile, StandardCharsets.UTF_8)) {
      BlockPalette palette = new BlockPalette();
      String line;
      int maxId = 0;

      while ((line = reader.readLine()) != null) {
        line = line.stripTrailing();
        if (line.isEmpty()) continue;

        int equalsIndex = line.indexOf('=');
        if (equalsIndex == -1) {
          throw new InvalidPaletteException("Invalid palette entry: " + line);
        }

        // NumberFormatException
        int id = Integer.parseInt(line, 0, equalsIndex, 16);
        maxId = Math.max(maxId, id);

        String blockStateString = line.substring(equalsIndex + 1);

        CompoundTag nbt = null;

        Path nbtFile = nbtDir.resolve(id + (useSnbt ? ".snbt" : ".nbt"));

        if (Files.exists(nbtFile)) {
          if (useSnbt) {
            String snbt = Files.readString(nbtFile);
            // CommandSyntaxException
            nbt = TagParser.parseCompoundFully(snbt);
          } else {
            nbt = NbtIo.read(nbtFile);
          }
        }

        Data data = new Data(blockStateString, nbt);

        if (palette.byId.containsKey(id)) {
          ParcelFormat.LOGGER.error(
              "Duplicate id {} in palette file {}. Did someone tweak the file by hand? ",
              id,
              paletteFile);
        }

        palette.byId.put(id, data);
        palette.byData.put(data, id);

        if (nbt != null) {
          palette.blockEntities.add(id);
        }
      }

      palette.nextId = maxId + 1;
      return palette;
    }
  }

  public static class InvalidPaletteException extends ParcelException.InvalidParcel {
    public InvalidPaletteException(String message) {
      super(message);
    }
  }
}
