package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.BLOCKS_DIR_NAME;
import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.PALETTE_FILE_NAME;
import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.SECTIONS_DIR_NAME;
import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.SECTION_BLOCK_ENTITY_SUFFIX;
import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.SECTION_BLOCK_STATE_SUFFIX;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSectionRegion;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponent;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponents;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.impl.parcel.BlockSectionPartitioner;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaRecordCodecs.BlockEntities;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.Config;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class ParcellaBlockCodec implements ParcellaComponentCodec {
  private final int sectionSize;
  private final ParcellaDigitCodec digitCodec;

  ParcellaBlockCodec(int sectionSize, ParcellaDigitCodec digitCodec) {
    this.sectionSize = sectionSize;
    this.digitCodec = digitCodec;
  }

  @Override
  public ParcelDataComponent component() {
    return ParcelDataComponents.BLOCKS;
  }

  @Override
  public String directoryName() {
    return BLOCKS_DIR_NAME;
  }

  @Override
  public void write(
      ParcelFormat.WriteContext<Config> context, ParcelDataSource source, Path directory)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    Path sectionsDirectory = directory.resolve(SECTIONS_DIR_NAME);
    BlockPalette palette =
        config.usePalette.get() ? loadPreviousPalette(directory.resolve(PALETTE_FILE_NAME)) : null;
    var output = new ParcellaCodecSupport.ManagedOutput(directory);
    long[] count = {0};
    source.forEachBlockSection(
        sectionSize,
        section -> {
          writeSection(config, palette, sectionsDirectory, section, output);
          context.progress().report("format_blocks", ++count[0], "sections");
        });
    if (palette != null) {
      palette.save(output.file(PALETTE_FILE_NAME));
    }
    output.finish();
  }

  @Override
  public void read(
      ParcelFormat.ReadContext<Config> context, ParcelDataSink sink, Path directory)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    Path sectionsDirectory = directory.resolve(SECTIONS_DIR_NAME);
    if (!Files.isDirectory(sectionsDirectory)) {
      throw new ParcelException.CorruptedParcelException(
          "Blocks directory not found: " + sectionsDirectory);
    }
    BlockPalette palette =
        Files.exists(directory.resolve(PALETTE_FILE_NAME))
            ? loadPalette(directory.resolve(PALETTE_FILE_NAME))
            : null;
    var expectedFiles = new HashSet<Path>();
    if (palette != null) expectedFiles.add(directory.resolve(PALETTE_FILE_NAME));
    Vec3i anchor = context.anchor();
    long count = 0;
    for (BlockSectionRegion section :
        BlockSectionPartitioner.partition(context.parcelSize(), anchor, sectionSize)) {
      BlockPos relativeOrigin =
          new BlockPos(
              section.origin().getX() - anchor.getX(),
              section.origin().getY() - anchor.getY(),
              section.origin().getZ() - anchor.getZ());
      long index = ZOrder3D.coordToIndexSigned(section.gridCoordinate(sectionSize, anchor));
      Path stateFile =
          RadixTreePathGenerator.toPath(
              sectionsDirectory, index, SECTION_BLOCK_STATE_SUFFIX);
      if (!Files.exists(stateFile)) {
        throw new ParcelException.CorruptedParcelException(
            "Missing block section: " + stateFile);
      }
      expectedFiles.add(stateFile);
      Path blockEntityFile =
          RadixTreePathGenerator.toPath(
              sectionsDirectory, index, SECTION_BLOCK_ENTITY_SUFFIX);
      List<BlockState> states =
          switch (config.blockStateEncoding.get()) {
            case FLAT -> decodeFlat(palette, stateFile, section);
            case RLE3D -> decodeRle(palette, stateFile, section);
          };
      List<BlockEntityRecord> blockEntities =
          Files.exists(blockEntityFile)
              ? ParcellaRecordCodecs.decode(
                      ParcellaRecordCodecs.BLOCK_ENTITIES,
                      ParcellaCodecSupport.readTag(
                          config.blockEntityDataFormat.get(), blockEntityFile))
                  .entries()
              : List.of();
      if (Files.exists(blockEntityFile)) expectedFiles.add(blockEntityFile);
      sink.acceptBlockSection(
          new BlockSection(relativeOrigin, section.size(), states, blockEntities));
      context.progress().report("format_blocks", ++count, "sections");
    }
    ParcellaCodecSupport.validateComponentFiles(directory, expectedFiles);
  }

  private void writeSection(
      Config config,
      BlockPalette palette,
      Path sectionsDirectory,
      BlockSection section,
      ParcellaCodecSupport.ManagedOutput output)
      throws IOException {
    Vec3i coordinate =
        new Vec3i(
            Math.floorDiv(section.origin().getX(), sectionSize),
            Math.floorDiv(section.origin().getY(), sectionSize),
            Math.floorDiv(section.origin().getZ(), sectionSize));
    long index = ZOrder3D.coordToIndexSigned(coordinate);
    Path stateFile =
        output.file(
            RadixTreePathGenerator.toPath(
                sectionsDirectory, index, SECTION_BLOCK_STATE_SUFFIX));
    String encoded =
        switch (config.blockStateEncoding.get()) {
          case FLAT -> encodeFlat(section, palette);
          case RLE3D -> encodeRle(section, palette);
        };
    Files.writeString(stateFile, encoded, StandardCharsets.UTF_8);

    if (!section.blockEntities().isEmpty()) {
      Path blockEntityFile =
          output.file(
              RadixTreePathGenerator.toPath(
                  sectionsDirectory, index, SECTION_BLOCK_ENTITY_SUFFIX));
      var sorted = new ArrayList<>(section.blockEntities());
      sorted.sort(
          Comparator.comparingInt((BlockEntityRecord entry) -> entry.pos().getY())
              .thenComparingInt(entry -> entry.pos().getX())
              .thenComparingInt(entry -> entry.pos().getZ()));
      config.blockEntityDataFormat.get().write(
          blockEntityFile,
          ParcellaRecordCodecs.encode(
              ParcellaRecordCodecs.BLOCK_ENTITIES, new BlockEntities(sorted)));
    }
  }

  private String encodeFlat(BlockSection section, BlockPalette palette) {
    var output = new StringBuilder(section.states().size() * 3);
    for (BlockState state : section.states()) {
      output.append(encodeState(state, palette)).append('\n');
    }
    return output.toString();
  }

  private String encodeRle(BlockSection section, BlockPalette palette) {
    var stateIds = new IdentityHashMap<BlockState, Integer>();
    var idStates = new ArrayList<BlockState>();
    var runs =
        VolumetricRLE.IMPL.encode(
            section.size().getX(),
            section.size().getY(),
            section.size().getZ(),
            (x, y, z) -> {
              BlockState state = section.state(x, y, z);
              if (palette != null) return palette.collect(state);
              return stateIds.computeIfAbsent(
                  state,
                  ignored -> {
                    idStates.add(state);
                    return idStates.size() - 1;
                  });
            });
    var output = new StringBuilder();
    for (var run : runs) {
      output
          .append(digitCodec.format(run.minX()))
          .append(digitCodec.format(run.minY()))
          .append(digitCodec.format(run.minZ()));
      if (run.minX() != run.maxX()
          || run.minY() != run.maxY()
          || run.minZ() != run.maxZ()) {
        output
            .append(digitCodec.format(run.maxX()))
            .append(digitCodec.format(run.maxY()))
            .append(digitCodec.format(run.maxZ()));
      }
      if (palette != null) {
        output.append('~').append(HexUtils.toHexUpperCase(run.value()));
      } else {
        output.append('=').append(BlockPalette.stringifyBlockState(idStates.get(run.value())));
      }
      output.append('\n');
    }
    return output.toString();
  }

  private static String encodeState(BlockState state, BlockPalette palette) {
    return palette == null
        ? BlockPalette.stringifyBlockState(state)
        : HexUtils.toHexUpperCase(palette.collect(state));
  }

  private static BlockPalette loadPreviousPalette(Path path) {
    if (Files.exists(path)) {
      try {
        return BlockPalette.load(path);
      } catch (Exception ignored) {
        // Invalid inherited data is replaced by the canonical result of the current capture.
      }
    }
    return new BlockPalette();
  }

  private static BlockPalette loadPalette(Path path)
      throws ParcelException.CorruptedParcelException {
    try {
      ParcellaCodecSupport.requireFileSize(
          path, ParcellaCodecSupport.MAX_SECTION_FILE_BYTES, "Block palette");
      return BlockPalette.load(path);
    } catch (Exception e) {
      throw new ParcelException.CorruptedParcelException("Invalid palette: " + path, e);
    }
  }

  private List<BlockState> decodeFlat(
      BlockPalette palette, Path path, BlockSectionRegion section)
      throws IOException, ParcelException.CorruptedParcelException {
    ParcellaCodecSupport.requireFileSize(
        path, ParcellaCodecSupport.MAX_SECTION_FILE_BYTES, "Block section");
    int expected = section.size().getX() * section.size().getY() * section.size().getZ();
    var states = new ArrayList<BlockState>(expected);
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while (states.size() < expected && (line = reader.readLine()) != null) {
        states.add(decodeState(line, palette, path));
      }
      if (states.size() != expected || reader.readLine() != null) {
        throw new ParcelException.CorruptedParcelException(
            "Expected exactly %d states in %s".formatted(expected, path));
      }
    }
    return states;
  }

  private List<BlockState> decodeRle(
      BlockPalette palette, Path path, BlockSectionRegion section)
      throws IOException, ParcelException.CorruptedParcelException {
    ParcellaCodecSupport.requireFileSize(
        path, ParcellaCodecSupport.MAX_SECTION_FILE_BYTES, "Block section");
    int count = section.size().getX() * section.size().getY() * section.size().getZ();
    var states =
        new ArrayList<BlockState>(
            java.util.Collections.nCopies(count, Blocks.AIR.defaultBlockState()));
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      int records = 0;
      while ((line = reader.readLine()) != null) {
        if (++records > count) {
          throw new ParcelException.CorruptedParcelException("Too many RLE records in " + path);
        }
        if (line.isBlank()) continue;
        int separator = findSeparator(line);
        if (separator != 3 && separator != 6) {
          throw new ParcelException.CorruptedParcelException("Invalid RLE line in " + path);
        }
        int x0 = parseDigit(line.charAt(0), path);
        int y0 = parseDigit(line.charAt(1), path);
        int z0 = parseDigit(line.charAt(2), path);
        int x1 = separator == 3 ? x0 : parseDigit(line.charAt(3), path);
        int y1 = separator == 3 ? y0 : parseDigit(line.charAt(4), path);
        int z1 = separator == 3 ? z0 : parseDigit(line.charAt(5), path);
        BlockState state = decodeState(line.substring(separator + 1), palette, path);
        validateRange(section, x0, y0, z0, x1, y1, z1, path);
        for (int x = x0; x <= x1; x++) {
          for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
              states.set(
                  (x * section.size().getY() + y) * section.size().getZ() + z, state);
            }
          }
        }
      }
    }
    return states;
  }

  private BlockState decodeState(String value, BlockPalette palette, Path path)
      throws ParcelException.CorruptedParcelException {
    if (palette != null && value.chars().allMatch(ParcellaBlockCodec::isHexDigit)) {
      int id;
      try {
        id = Integer.parseUnsignedInt(value, 16);
      } catch (NumberFormatException e) {
        throw new ParcelException.CorruptedParcelException("Invalid palette id in " + path, e);
      }
      BlockState state = palette.get(id);
      if (state == null) {
        throw new ParcelException.CorruptedParcelException(
            "Unknown palette id %s in %s".formatted(value, path));
      }
      return state;
    }
    var result = BlockPalette.parseBlockState(value);
    if (result.isErr()) {
      throw new ParcelException.CorruptedParcelException(
          "Invalid block state in %s: %s".formatted(path, result.unwrapErr()));
    }
    return result.unwrap();
  }

  private static int findSeparator(String line) {
    int palette = line.indexOf('~');
    int inline = line.indexOf('=');
    if (palette < 0) return inline;
    if (inline < 0) return palette;
    return Math.min(palette, inline);
  }

  private int parseDigit(char value, Path path)
      throws ParcelException.CorruptedParcelException {
    int parsed = digitCodec.parse((byte) value);
    if (parsed < 0) {
      throw new ParcelException.CorruptedParcelException(
          "Invalid coordinate digit '%s' in %s".formatted(value, path));
    }
    return parsed;
  }

  private static void validateRange(
      BlockSectionRegion section,
      int x0,
      int y0,
      int z0,
      int x1,
      int y1,
      int z1,
      Path path)
      throws ParcelException.CorruptedParcelException {
    if (x0 > x1
        || y0 > y1
        || z0 > z1
        || x1 >= section.size().getX()
        || y1 >= section.size().getY()
        || z1 >= section.size().getZ()) {
      throw new ParcelException.CorruptedParcelException("RLE range is outside " + path);
    }
  }

  private static boolean isHexDigit(int value) {
    return HexUtils.parseChar((char) value) >= 0;
  }
}
