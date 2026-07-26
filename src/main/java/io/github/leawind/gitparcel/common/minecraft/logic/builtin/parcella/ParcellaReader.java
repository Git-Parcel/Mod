package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSink;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ParcellaUtils;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared streaming reader for all Parcella grid sizes. */
public abstract class ParcellaReader
    implements ParcelFormat.Reader<ParcellaFormat.Config>, ParcellaFormat {
  private static final int MAX_DIRECTORY_RECORDS = 100_000;
  private static final long MAX_SECTION_FILE_BYTES = 16L * 1024 * 1024;
  private static final long MAX_RECORD_FILE_BYTES = 64L * 1024 * 1024;

  private final int sectionSize;
  private final ParcellaDigitCodec digitCodec;

  protected ParcellaReader(int sectionSize, ParcellaDigitCodec digitCodec) {
    this.sectionSize = sectionSize;
    this.digitCodec = digitCodec;
  }

  @Override
  public void read(ReadContext<Config> context, ParcelContentSink sink)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    Path blocksDir = context.dataDir().resolve(BLOCKS_DIR_NAME);
    Path sectionsDir = blocksDir.resolve(SUBPARCELS_DIR_NAME);
    if (!Files.isDirectory(sectionsDir)) {
      throw new ParcelException.CorruptedParcelException(
          "Blocks directory not found: " + sectionsDir);
    }

    BlockPalette palette =
        Files.exists(blocksDir.resolve(PALETTE_FILE_NAME))
            ? loadPalette(blocksDir.resolve(PALETTE_FILE_NAME))
            : null;

    long[] attachmentCount = {0};
    readDirectory(
        context.dataDir().resolve(ATTACHMENTS_DIR_NAME),
        ".snbt",
        path -> {
          CompoundTag tag = readTag(NbtFormat.TEXT, path);
          sink.acceptAttachment(
              ParcellaRecordCodecs.decode(ParcellaRecordCodecs.ATTACHMENT, tag));
          context.progress().report("format_attachments", ++attachmentCount[0], "attachments");
        });

    Vec3i anchor = context.anchor();
    long sectionCount = 0;
    for (var section :
        ParcellaUtils.subdivideParcel(context.parcelSize(), context.anchor(), sectionSize)) {
      BlockPos relativeOrigin =
          new BlockPos(
              section.originX - anchor.getX(),
              section.originY - anchor.getY(),
              section.originZ - anchor.getZ());
      Vec3i coord =
          new Vec3i(
              Math.floorDiv(relativeOrigin.getX(), sectionSize),
              Math.floorDiv(relativeOrigin.getY(), sectionSize),
              Math.floorDiv(relativeOrigin.getZ(), sectionSize));
      long index = ZOrder3D.coordToIndexSigned(coord);
      Path stateFile =
          RadixTreePathGenerator.toPath(
              sectionsDir, index, SUBPARCEL_BLOCK_STATE_SUFFIX);
      if (!Files.exists(stateFile)) {
        throw new ParcelException.CorruptedParcelException(
            "Missing block section: " + stateFile);
      }
      Path blockEntityFile =
          RadixTreePathGenerator.toPath(
              sectionsDir, index, SUBPARCEL_BLOCK_ENTITY_SUFFIX);

      List<BlockState> states =
          switch (config.subparcelFormat.get()) {
            case FLAT -> decodeFlat(config, palette, stateFile, section);
            case RLE3D -> decodeRle(config, palette, stateFile, section);
          };
      List<BlockEntityRecord> blockEntities =
          Files.exists(blockEntityFile)
              ? ParcellaRecordCodecs.decode(
                      ParcellaRecordCodecs.BLOCK_ENTITIES,
                      readTag(config.blockEntityDataFormat.get(), blockEntityFile))
                  .entries()
              : List.of();

      sink.acceptBlockSection(
          new BlockSection(
              relativeOrigin,
              new Vec3i(section.sizeX, section.sizeY, section.sizeZ),
              states,
              blockEntities));
      context.progress().report("format_blocks", ++sectionCount, "sections");
    }

    NbtFormat entityFormat = config.entityDataFormat.get();
    long[] entityCount = {0};
    readDirectory(
        context.dataDir().resolve(ENTITIES_DIR_NAME),
        entityFormat.getSuffix(),
        path ->
            {
              sink.acceptEntity(
                  ParcellaRecordCodecs.decode(
                      ParcellaRecordCodecs.ENTITY, readTag(entityFormat, path)));
              context.progress().report("format_entities", ++entityCount[0], "entities");
            });
    sink.finish();
  }

  private List<BlockState> decodeFlat(
      Config config, BlockPalette palette, Path path, Subparcel section)
      throws IOException, ParcelException.CorruptedParcelException {
    requireFileSize(path, MAX_SECTION_FILE_BYTES, "Block section");
    int expected = section.sizeX * section.sizeY * section.sizeZ;
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
      Config config, BlockPalette palette, Path path, Subparcel section)
      throws IOException, ParcelException.CorruptedParcelException {
    requireFileSize(path, MAX_SECTION_FILE_BYTES, "Block section");
    int count = section.sizeX * section.sizeY * section.sizeZ;
    var states = new ArrayList<BlockState>(java.util.Collections.nCopies(count, Blocks.AIR.defaultBlockState()));
    try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      int records = 0;
      while ((line = reader.readLine()) != null) {
        if (++records > count) {
          throw new ParcelException.CorruptedParcelException(
              "Too many RLE records in " + path);
        }
        if (line.isBlank()) {
          continue;
        }
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
              states.set((x * section.sizeY + y) * section.sizeZ + z, state);
            }
          }
        }
      }
    }
    return states;
  }

  private BlockState decodeState(String value, BlockPalette palette, Path path)
      throws ParcelException.CorruptedParcelException {
    if (palette != null && value.chars().allMatch(ParcellaReader::isHexDigit)) {
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
      Subparcel section, int x0, int y0, int z0, int x1, int y1, int z1, Path path)
      throws ParcelException.CorruptedParcelException {
    if (x0 > x1
        || y0 > y1
        || z0 > z1
        || x1 >= section.sizeX
        || y1 >= section.sizeY
        || z1 >= section.sizeZ) {
      throw new ParcelException.CorruptedParcelException("RLE range is outside " + path);
    }
  }

  private static boolean isHexDigit(int value) {
    return HexUtils.parseChar((char) value) >= 0;
  }

  private static BlockPalette loadPalette(Path path)
      throws ParcelException.CorruptedParcelException {
    try {
      requireFileSize(path, MAX_SECTION_FILE_BYTES, "Block palette");
      return BlockPalette.load(path);
    } catch (Exception e) {
      throw new ParcelException.CorruptedParcelException("Invalid palette: " + path, e);
    }
  }

  private static CompoundTag readTag(NbtFormat format, Path path)
      throws ParcelException.CorruptedParcelException {
    try {
      requireFileSize(path, MAX_RECORD_FILE_BYTES, "NBT record");
    } catch (IOException e) {
      throw new ParcelException.CorruptedParcelException(
          "Failed to inspect record size: " + path, e);
    }
    var result = format.read(path);
    if (result.isErr()) {
      throw new ParcelException.CorruptedParcelException(
          "Failed to read %s: %s".formatted(path, result.unwrapErr()));
    }
    return result.unwrap();
  }

  private static void requireFileSize(Path path, long maximum, String type) throws IOException {
    if (Files.size(path) > maximum) {
      throw new IOException(type + " exceeds the format size limit: " + path);
    }
  }

  private static void readDirectory(Path directory, String suffix, FileConsumer consumer)
      throws IOException, ParcelException {
    if (!Files.isDirectory(directory)) {
      return;
    }
    var matching = new ArrayList<Path>();
    try (var paths = Files.list(directory)) {
      var iterator = paths.iterator();
      while (iterator.hasNext()) {
        Path path = iterator.next();
        if (Files.isRegularFile(path)
            && path.getFileName().toString().endsWith(suffix)) {
          if (matching.size() >= MAX_DIRECTORY_RECORDS) {
            throw new ParcelException.CorruptedParcelException(
                "Too many records in directory: " + directory);
          }
          matching.add(path);
        }
      }
    }
    matching.sort(Comparator.comparing(item -> item.getFileName().toString()));
    for (Path path : matching) {
      consumer.accept(path);
    }
  }

  @FunctionalInterface
  private interface FileConsumer {
    void accept(Path path) throws IOException, ParcelException;
  }
}
