package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import io.github.leawind.gitparcel.common.api.config.ConfigItemBuilder;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSectionRegion;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.impl.parcel.BlockSectionPartitioner;
import io.github.leawind.gitparcel.common.impl.content.ParcelRecordCodecs.BlockEntities;
import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Built-in storage and streaming behavior for block states and block entities. */
public final class BlockContentType implements ParcelContentType<BlockContentType.Config> {
  public static final String ID = "blocks";
  public static final Spec SPEC = new Spec(ID, 1);
  private static final String PALETTE_FILE_NAME = "palette.txt";
  private static final String SECTIONS_DIR_NAME = "sections";
  private static final String SECTION_BLOCK_STATE_SUFFIX = ".txt";
  private static final String SECTION_BLOCK_ENTITY_SUFFIX = ".be.snbt";

  @Override
  public Spec spec() {
    return SPEC;
  }

  @Override
  public Set<String> loadAfter() {
    return Set.of(AttachmentContentType.ID);
  }

  @Override
  public Config defaultConfig() {
    return new Config();
  }

  @Override
  public Class<Config> configClass() {
    return Config.class;
  }

  @Override
  public void save(SaveContext<Config> context, ParcelDataSource source)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    BlockSectionSize configuredSize = config.sectionSize.get();
    int sectionSize = configuredSize.edgeLength();
    BlockStateDigitCodec digitCodec = configuredSize.digitCodec();
    Path directory = context.directory();
    Path sectionsDirectory = directory.resolve(SECTIONS_DIR_NAME);
    BlockPalette palette = loadPreviousPalette(directory.resolve(PALETTE_FILE_NAME));
    var output = new ParcelContentFileSupport.ManagedOutput(directory);
    long[] count = {0};
    source.forEachBlockSection(
        sectionSize,
        section -> {
          writeSection(
              sectionSize, digitCodec, palette, sectionsDirectory, section, output);
          context.progress().report("content_blocks", ++count[0], "sections");
        });
    palette.save(output.file(PALETTE_FILE_NAME));
    output.finish();
  }

  @Override
  public void load(LoadContext<Config> context, ParcelDataSink sink)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    BlockSectionSize configuredSize = config.sectionSize.get();
    int sectionSize = configuredSize.edgeLength();
    BlockStateDigitCodec digitCodec = configuredSize.digitCodec();
    Path directory = context.directory();
    Path sectionsDirectory = directory.resolve(SECTIONS_DIR_NAME);
    if (!Files.isDirectory(sectionsDirectory)) {
      throw new ParcelException.CorruptedParcelException(
          "Blocks directory not found: " + sectionsDirectory);
    }
    Path paletteFile = directory.resolve(PALETTE_FILE_NAME);
    if (!Files.isRegularFile(paletteFile)) {
      throw new ParcelException.CorruptedParcelException(
          "Block palette not found: " + paletteFile);
    }
    BlockPalette palette = loadPalette(paletteFile);
    var expectedFiles = new HashSet<Path>();
    expectedFiles.add(paletteFile);
    Vec3i anchor = context.anchor();
    long count = 0;
    for (BlockSectionRegion section :
        BlockSectionPartitioner.partition(context.parcelSize(), anchor, sectionSize)) {
      BlockPos relativeOrigin =
          new BlockPos(
              section.origin().getX() - anchor.getX(),
              section.origin().getY() - anchor.getY(),
              section.origin().getZ() - anchor.getZ());
      // Load partitions are parcel-relative, so the anchor shift belongs in the grid index; this
      // matches the save path, whose sections already arrive anchor-relative.
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
      List<BlockState> states = decodeRle(digitCodec, palette, stateFile, section);
      List<BlockEntityRecord> blockEntities =
          Files.exists(blockEntityFile)
              ? ParcelRecordCodecs.decode(
                      ParcelRecordCodecs.BLOCK_ENTITIES,
                      ParcelContentFileSupport.readTag(NbtFormat.TEXT, blockEntityFile))
                  .entries()
              : List.of();
      if (Files.exists(blockEntityFile)) expectedFiles.add(blockEntityFile);
      sink.acceptBlockSection(
          new BlockSection(relativeOrigin, section.size(), states, blockEntities));
      context.progress().report("content_blocks", ++count, "sections");
    }
    ParcelContentFileSupport.validateOwnedFiles(directory, expectedFiles);
  }

  private void writeSection(
      int sectionSize,
      BlockStateDigitCodec digitCodec,
      BlockPalette palette,
      Path sectionsDirectory,
      BlockSection section,
      ParcelContentFileSupport.ManagedOutput output)
      throws IOException {
    // Sections arrive with anchor-relative origins, so their coordinates are already aligned to
    // the same lattice as the load path's floorDiv(origin - anchor, sectionSize) grid index.
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
    encodeRle(digitCodec, section, palette, stateFile);

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
      NbtFormat.TEXT.write(
          blockEntityFile,
          ParcelRecordCodecs.encode(
              ParcelRecordCodecs.BLOCK_ENTITIES, new BlockEntities(sorted)));
    }
  }

  private void encodeRle(
      BlockStateDigitCodec digitCodec, BlockSection section, BlockPalette palette, Path stateFile)
      throws IOException {
    var runs =
        VolumetricRLE.IMPL.encode(
            section.size().getX(),
            section.size().getY(),
            section.size().getZ(),
            (x, y, z) -> palette.collect(section.state(x, y, z)));
    try (var writer =
        java.nio.file.Files.newBufferedWriter(stateFile, java.nio.charset.StandardCharsets.UTF_8)) {
      for (var run : runs) {
        writer.write(digitCodec.format(run.minX()));
        writer.write(digitCodec.format(run.minY()));
        writer.write(digitCodec.format(run.minZ()));
        if (run.minX() != run.maxX()
            || run.minY() != run.maxY()
            || run.minZ() != run.maxZ()) {
          writer.write(digitCodec.format(run.maxX()));
          writer.write(digitCodec.format(run.maxY()));
          writer.write(digitCodec.format(run.maxZ()));
        }
        writer.write('~');
        writer.write(HexUtils.toHexUpperCase(run.value()));
        writer.write('\n');
      }
    }
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
      ParcelContentFileSupport.requireFileSize(
          path, ParcelContentFileSupport.MAX_SECTION_FILE_BYTES, "Block palette");
      return BlockPalette.load(path);
    } catch (Exception e) {
      throw new ParcelException.CorruptedParcelException("Invalid palette: " + path, e);
    }
  }

  private List<BlockState> decodeRle(
      BlockStateDigitCodec digitCodec,
      BlockPalette palette,
      Path path,
      BlockSectionRegion section)
      throws IOException, ParcelException.CorruptedParcelException {
    ParcelContentFileSupport.requireFileSize(
        path, ParcelContentFileSupport.MAX_SECTION_FILE_BYTES, "Block section");
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
        }        int x0 = parseDigit(digitCodec, line.charAt(0), path);
        int y0 = parseDigit(digitCodec, line.charAt(1), path);
        int z0 = parseDigit(digitCodec, line.charAt(2), path);
        int x1 = separator == 3 ? x0 : parseDigit(digitCodec, line.charAt(3), path);
        int y1 = separator == 3 ? y0 : parseDigit(digitCodec, line.charAt(4), path);
        int z1 = separator == 3 ? z0 : parseDigit(digitCodec, line.charAt(5), path);
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
    if (value.isEmpty() || !value.chars().allMatch(BlockContentType::isHexDigit)) {
      throw new ParcelException.CorruptedParcelException(
          "Invalid palette id in %s: %s".formatted(path, value));
    }
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

  private static int findSeparator(String line) {
    return line.indexOf('~');
  }

  private static int parseDigit(BlockStateDigitCodec digitCodec, char value, Path path)
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

  public enum BlockSectionSize {
    SIZE_16(16, BlockStateDigitCodec.HEX16),
    SIZE_32(32, BlockStateDigitCodec.BASE32);

    private final int edgeLength;
    private final BlockStateDigitCodec digitCodec;

    BlockSectionSize(int edgeLength, BlockStateDigitCodec digitCodec) {
      this.edgeLength = edgeLength;
      this.digitCodec = digitCodec;
    }

    public int edgeLength() {
      return edgeLength;
    }

    BlockStateDigitCodec digitCodec() {
      return digitCodec;
    }
  }

  public static final class Config extends ParcelContentConfig<Config> {
    public final ConfigItem<BlockSectionSize> sectionSize =
        ConfigItemBuilder.ofEnum("sectionSize", BlockSectionSize.SIZE_32)
            .storeLocally()
            .build();

    public Config() {
      register(sectionSize);
    }
  }
}
