package io.github.leawind.gitparcel.parcelformats.parcella;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.api.parcel.ParcelStorage;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.mixin.AccessStateHolder;
import io.github.leawind.gitparcel.parcelformats.NbtFormat;
import io.github.leawind.gitparcel.utils.IntIdPalette;
import io.github.leawind.gitparcel.utils.numbase.HexUtils;
import io.github.leawind.inventory.just.Result;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
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
 * <p>Example:
 *
 * <pre>
 *     0=minecraft:air
 *     1=minecraft:stone
 *     2>minecraft:chest
 * </pre>
 */
public class BlockPalette extends IntIdPalette<BlockPalette.Data> {
  protected final IntSet blockEntities = new IntOpenHashSet();

  /**
   * Stores visited block ids.
   *
   * <p>Updated every time a block is collected.
   *
   * @see #onAfterInserted(int, Data)
   */
  protected final IntSet visited = new IntOpenHashSet();

  public BlockPalette() {
    super();
  }

  public int collect(BlockState blockState, @Nullable CompoundTag nbt) {
    return collect(new Data(blockState, nbt));
  }

  @Override
  public void onAfterInserted(int id, @Nullable Data data) {
    visited.add(id);
    if (data != null && data.hasNbt()) {
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

    var sb = new StringBuilder();
    for (var entry : byId.int2ObjectEntrySet()) {
      int id = entry.getIntKey();
      if (!(visited.contains(id) && isIdInUse(id))) {
        continue;
      }

      sb.append(HexUtils.toHexUpperCase(id));
      Data data = entry.getValue();
      sb.append(data.hasNbt() ? '>' : '=');
      sb.append(stringifyBlockState(data.blockState));
      sb.append('\n');
    }

    Files.writeString(paletteFile, sb, StandardCharsets.UTF_8);

    // Save NBTs
    for (int id : blockEntities) {
      Data data = byId.get(id);
      if (data.hasNbt()) {
        var nbtFile = NbtFilePath.resolve(nbtDir, nbtFormat, id);
        nbtFormat.write(nbtFile, data.nbt, false);
      } else {
        ParcelStorage.LOGGER.error(
            "Block {} was marked as block entity, but it has no NBT data", id);
      }
    }
  }

  public interface NbtFilePath {
    static Path resolve(Path nbtDir, NbtFormat nbtFormat, int paletteId) {
      return nbtDir.resolve(HexUtils.toHexUpperCase(paletteId) + nbtFormat.suffix);
    }
  }

  /**
   * @see StateHolder#toString
   * @see BlockStateParser#parseForBlock
   */
  public static String stringifyBlockState(BlockState blockState) {
    var sb = new StringBuilder();
    sb.append(BuiltInRegistries.BLOCK.wrapAsHolder(blockState.getBlock()).getRegisteredName());
    if (!blockState.getValues().isEmpty()) {
      sb.append('[');
      sb.append(
          blockState.getValues().entrySet().stream()
              .map(AccessStateHolder.getPropertyEntryToStringFunction())
              .collect(Collectors.joining(",")));
      sb.append(']');
    }
    return sb.toString();
  }

  public static Result<BlockState, String> parseBlockState(
      String blockStateString, LevelReader level, boolean allowNbt) {
    try {
      var blockState =
          BlockStateParser.parseForBlock(
                  level.holderLookup(Registries.BLOCK), blockStateString, allowNbt)
              .blockState();
      return Result.ok(blockState);
    } catch (CommandSyntaxException e) {
      return Result.err(e.getMessage());
    }
  }

  public record Data(BlockState blockState, @Nullable CompoundTag nbt) {
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
   */
  public static @Nullable BlockPalette loadIfExist(
      LevelAccessor level, Path paletteFile, Path nbtDir, NbtFormat nbtFormat)
      throws IOException, InvalidPaletteException {
    if (!Files.exists(paletteFile)) {
      return null;
    }
    return load(level, paletteFile, nbtDir, nbtFormat);
  }

  public static final Pattern ROW_PATTERN = Pattern.compile("^([0-9a-fA-F]+)([>=])(.*)");
  public static final char NO_NBT_MARKER = '=';
  public static final char NBT_MARKER = '>';

  /**
   * Loads a block palette from the specified file and NBT directory.
   *
   * <p>The palette's lastId will be the last id in the palette file, which is expected to be the
   * highest one if the palette file was correctly saved.
   *
   * @param paletteFile the path to the palette file. Must exist.
   * @param nbtDir the directory to store NBT files
   * @param nbtFormat the format to use for NBT files
   * @return the loaded block palette
   * @throws IOException if an I/O error occurs
   * @throws InvalidPaletteException if the palette file is malformed
   */
  public static BlockPalette load(
      LevelAccessor level, Path paletteFile, Path nbtDir, NbtFormat nbtFormat)
      throws IOException, InvalidPaletteException {

    record ManifestEntry(int num, int id, String blockStateString, boolean hasNbt) {
      static List<ManifestEntry> parse(BufferedReader reader)
          throws IOException, InvalidPaletteException {
        List<ManifestEntry> entries = new ArrayList<>();

        String line;
        int row = 0;
        while ((line = reader.readLine()) != null) {
          row++;

          if (line.isEmpty()) {
            continue;
          }

          var matcher = ROW_PATTERN.matcher(line);
          if (matcher.matches()) {
            int id = Integer.parseInt(matcher.group(1), 16);
            boolean hasNbt = matcher.group(2).charAt(0) == NBT_MARKER;
            String blockStateString = matcher.group(3);

            entries.add(new ManifestEntry(row, id, blockStateString, hasNbt));
          } else {
            throw new InvalidPaletteException(
                String.format("Invalid line format at row %d: %s", row, line));
          }
        }

        return entries;
      }
    }

    var palette = new BlockPalette();

    List<ManifestEntry> entries;
    try (var reader = Files.newBufferedReader(paletteFile, StandardCharsets.UTF_8)) {
      entries = ManifestEntry.parse(reader);
    }

    for (var entry : entries) {
      var blockState =
          switch (parseBlockState(entry.blockStateString, level, false)) {
            case Result.Ok(BlockState value) -> value;
            case Result.Err(String msg) -> {
              ParcelStorage.LOGGER.error(
                  "Skip because failed to parse block state '{}': {}", entry.blockStateString, msg);
              yield null;
            }
          };

      CompoundTag nbt = null;
      if (entry.hasNbt) {
        Path nbtFile = nbtDir.resolve(HexUtils.toHexUpperCase(entry.id) + nbtFormat.suffix);

        nbt =
            switch (nbtFormat.read(nbtFile)) {
              case Result.Ok(CompoundTag value) -> value;
              case Result.Err(String msg) -> {
                ParcelStorage.LOGGER.error("Failed to read NBT file {}: {}", nbtFile, msg);
                yield null;
              }
            };
      }

      palette.insert(entry.id, new Data(blockState, nbt));
      palette.lastId = entry.id;
    }

    return palette;
  }

  public static class InvalidPaletteException extends ParcelException.CorruptedParcelException {
    public InvalidPaletteException(String message) {
      super(message);
    }
  }
}
