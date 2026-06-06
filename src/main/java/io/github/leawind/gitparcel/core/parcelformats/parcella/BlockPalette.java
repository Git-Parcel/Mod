package io.github.leawind.gitparcel.core.parcelformats.parcella;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.leawind.gitparcel.core.api.parcel.exceptions.ParcelException;
import io.github.leawind.gitparcel.mc.mixin.AccessStateHolder;
import io.github.leawind.gitparcel.mc.storage.ParcelStorage;
import io.github.leawind.gitparcel.util.IntIdPalette;
import io.github.leawind.gitparcel.util.numbase.HexUtils;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import org.jspecify.annotations.Nullable;

/**
 * Block palette containing block states
 *
 * <p>Example:
 *
 * <pre>
 *     0=minecraft:air
 *     1=minecraft:stone
 * </pre>
 */
public class BlockPalette extends IntIdPalette<BlockState> {

  protected final IntSet visited = new IntOpenHashSet();

  public BlockPalette() {
    super();
  }

  @Override
  public void onAfterInserted(int id, @Nullable BlockState data) {
    visited.add(id);
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
   * Saves this block palette to the specified file.
   *
   * @param paletteFile the path to the palette file. Its parent directory will be created if it
   *     does not exist.
   * @throws IOException if an I/O error occurs while saving the palette
   */
  public void save(Path paletteFile) throws IOException {
    Files.createDirectories(paletteFile.getParent());

    var sb = new StringBuilder();
    for (var entry : byId.int2ObjectEntrySet()) {
      int id = entry.getIntKey();
      if (!(visited.contains(id) && isIdInUse(id))) {
        continue;
      }

      sb.append(HexUtils.toHexUpperCase(id));
      sb.append('=');
      sb.append(stringifyBlockState(entry.getValue()));
      sb.append('\n');
    }

    Files.writeString(paletteFile, sb, StandardCharsets.UTF_8);
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

  public static Result<BlockState, String> parseBlockState(String blockStateString) {
    try {
      var blockState =
          BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, blockStateString, false)
              .blockState();
      return Result.ok(blockState);
    } catch (CommandSyntaxException e) {
      return Result.err(e.getMessage());
    }
  }

  public static final Pattern ROW_PATTERN = Pattern.compile("^([0-9a-fA-F]+)=(.*)");

  /**
   * Loads a block palette from the specified file.
   *
   * @param paletteFile the path to the palette file. Must exist.
   * @return the loaded block palette
   * @throws IOException if an I/O error occurs
   * @throws InvalidPaletteException if the palette file is malformed
   */
  public static BlockPalette load(Path paletteFile) throws IOException, InvalidPaletteException {

    record ManifestEntry(int row, int id, String blockStateString) {
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
            String blockStateString = matcher.group(2);

            entries.add(new ManifestEntry(row, id, blockStateString));
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
          switch (parseBlockState(entry.blockStateString)) {
            case Result.Ok(BlockState value) -> value;
            case Result.Err(String msg) -> {
              ParcelStorage.LOGGER.error(
                  "Skip because failed to parse block state '{}': {}", entry.blockStateString, msg);
              yield null;
            }
          };

      palette.insert(entry.id, blockState);
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
