package io.github.leawind.gitparcel.parcel.formats.parcella;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.parcel.ParcelFormat;
import io.github.leawind.gitparcel.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.parcel.formats.NbtFormat;
import io.github.leawind.gitparcel.utils.IntIdPalette;
import io.github.leawind.gitparcel.utils.hex.HexUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Block palette
 *
 * <p>Data: BlockState string + Optional NBT Data
 *
 * <p>Id range: [0, {@value Integer#MAX_VALUE}]
 *
 * <p>Ids of block without NBT data are marked with {@code =}.
 *
 * <p>Ids of block entities are marked with {@code >}.
 *
 * <p>Unused ids are marked with {@code ?}.
 *
 * <p>Example:
 *
 * <pre>
 *     0=minecraft:air
 *     1=minecraft:stone
 *     2>minecraft:chest
 *     3?
 * </pre>
 */
public class BlockPalette extends IntIdPalette<BlockPalette.Data> {
  protected final Set<Integer> blockEntities = new IntArraySet();

  /**
   * Stores visited block ids.
   *
   * <p>Updated every time a block is collected.
   *
   * @see #onAfterInserted(int, Data)
   */
  protected final Set<Integer> visited = new IntArraySet();

  public BlockPalette() {
    super();
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
    String blockStateString =
        BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getRegisteredName();
    return collect(blockStateString, nbt);
  }

  public int collect(String blockStateString, @Nullable CompoundTag nbt) {
    return collect(new Data(blockStateString, nbt));
  }

  @Override
  public void onAfterInserted(int id, @Nullable Data data) {
    visited.add(id);
    if (data != null && data.nbt != null) {
      blockEntities.add(id);
    }
  }

  @Override
  public void onAfterRemoved(int id, Data data) {
    blockEntities.remove(id);
  }

  /** Clears all visited blocks. */
  public void clearVisited() {
    visited.clear();
  }

  /** Removes all unvisited blocks from this palette. */
  public void clearUnvisited() {
    for (int id : byId.keySet()) {
      if (!visited.contains(id)) {
        removeById(id);
      }
    }
  }

  /**
   * Saves this block palette to the specified file and NBT directory.
   *
   * @param paletteFile the path to the palette file. Its parent directory will be created if it
   *     does not exist.
   * @param nbtDir the directory to store NBT files. Will be created if it does not exist.
   * @param nbtFormat the format to use for NBT files
   * @throws IOException if an I/O error occurs while saving the palette
   */
  public void save(Path paletteFile, Path nbtDir, NbtFormat nbtFormat) throws IOException {
    Files.createDirectories(paletteFile.getParent());
    Files.createDirectories(nbtDir);
    try (BufferedWriter writer = Files.newBufferedWriter(paletteFile, StandardCharsets.UTF_8)) {
      var sb = new StringBuilder();
      for (var entry : byId.int2ObjectEntrySet()) {
        Data data = entry.getValue();

        sb.append(HexUtils.toHexUpperCase(entry.getIntKey()));
        if (data == null) {
          sb.append('?');
        } else {
          sb.append(data.hasNbt() ? '>' : '=');
          sb.append(data.blockStateString);
        }
        sb.append('\n');
      }

      writer.write(sb.toString());
    }
    // Save NBTs
    for (int id : blockEntities) {
      Data data = byId.get(id);
      if (data.nbt != null) {
        var nbtFile = nbtDir.resolve(HexUtils.toHexUpperCase(id) + nbtFormat.suffix);
        nbtFormat.write(nbtFile, data.nbt, true);
      }
    }
  }

  private static int getId(Int2ObjectMap.Entry<Data> entry) {
    return entry.getIntKey();
  }

  public record Data(String blockStateString, @Nullable CompoundTag nbt) {
    public boolean hasNbt() {
      return nbt != null;
    }
  }

  /**
   * Loads a block palette from the specified file and NBT directory if it exists.
   *
   * <p>If the palette file does not exist, {@code null} will be returned.
   *
   * @param paletteFile the path to the palette file. If not exist, {@code null} will be returned.
   * @param nbtDir the directory to store NBT files
   * @param nbtFormat the format to use for NBT files
   * @return the loaded block palette, or {@code null} if the palette file does not exist
   * @throws IOException if an I/O error occurs
   * @throws InvalidPaletteException if the palette file is malformed
   * @throws NumberFormatException if an ID in the palette file is not a valid hexadecimal number
   * @throws CommandSyntaxException if the snbt format is used and the NBT file is malformed
   */
  public static @Nullable BlockPalette loadIfExist(
      Path paletteFile, Path nbtDir, NbtFormat nbtFormat)
      throws IOException, InvalidPaletteException, NumberFormatException, CommandSyntaxException {
    if (!Files.exists(paletteFile)) {
      return null;
    }
    return load(paletteFile, nbtDir, nbtFormat);
  }

  /**
   * Loads a block palette from the specified file and NBT directory.
   *
   * <p>For duplicate IDs, only the first one will be used.
   *
   * @param paletteFile the path to the palette file. Must exist.
   * @param nbtDir the directory to store NBT files
   * @param nbtFormat the format to use for NBT files
   * @return the loaded block palette
   * @throws IOException if an I/O error occurs
   * @throws InvalidPaletteException if the palette file is malformed
   * @throws NumberFormatException if an ID in the palette file is not a valid hexadecimal number
   * @throws CommandSyntaxException if the NBT format is text and the NBT file is malformed
   */
  public static BlockPalette load(Path paletteFile, Path nbtDir, NbtFormat nbtFormat)
      throws IOException, InvalidPaletteException, NumberFormatException, CommandSyntaxException {
    try (var reader = Files.newBufferedReader(paletteFile, StandardCharsets.UTF_8)) {
      BlockPalette palette = new BlockPalette();

      String line;
      while ((line = reader.readLine()) != null) {

        char type = '\0';
        String idString = null;
        StringBuilder buffer = new StringBuilder(32);

        for (char ch : line.toCharArray()) {
          switch (ch) {
            case '=', '>', '?' -> {
              type = ch;
              idString = buffer.toString();
              buffer.setLength(0);
            }
            default -> buffer.append(ch);
          }
        }

        if (type == '\0') {
          throw new InvalidPaletteException(
              String.format(
                  "Invalid palette entry. No type char ( '=', '>', '?' ) found: %s", line));
        }

        int id = Integer.parseInt(idString, 16);

        if (palette.byId.containsKey(id)) {
          ParcelFormat.LOGGER.warn(
              "Duplicate id {} in palette file {}. Did someone tweak the file by hand? ",
              id,
              paletteFile);
          continue;
        }

        switch (type) {
          case '?' -> palette.insert(id, null);
          case '=' -> palette.insert(id, new Data(buffer.toString(), null));
          case '>' -> {
            Path nbtFile = nbtDir.resolve(idString + nbtFormat.suffix);
            CompoundTag nbt =
                switch (nbtFormat) {
                  case Binary -> NbtFormat.readBinary(nbtFile);
                  case Text -> NbtFormat.readText(nbtFile);
                };
            palette.insert(id, new Data(buffer.toString(), nbt));
          }
        }
      }

      return palette;
    }
  }

  public static class InvalidPaletteException extends ParcelException.InvalidParcel {
    public InvalidPaletteException(String message) {
      super(message);
    }
  }
}
