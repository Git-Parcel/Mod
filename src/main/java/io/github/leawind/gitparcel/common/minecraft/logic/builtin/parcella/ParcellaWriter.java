package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaRecordCodecs.BlockEntities;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.RadixTreePathGenerator;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.common.utils.io.NioFileTree;
import io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLE;
import io.github.leawind.gitparcel.common.utils.numbase.HexUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.state.BlockState;

/** Shared streaming writer for all Parcella grid sizes. */
public abstract class ParcellaWriter
    implements ParcelFormat.Writer<ParcellaFormat.Config>, ParcellaFormat {
  private final int sectionSize;
  private final ParcellaDigitCodec digitCodec;

  protected ParcellaWriter(int sectionSize, ParcellaDigitCodec digitCodec) {
    this.sectionSize = sectionSize;
    this.digitCodec = digitCodec;
  }

  @Override
  public int blockSectionSize() {
    return sectionSize;
  }

  @Override
  public void write(
      WriteContext<Config> context,
      io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSource source)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    Path blocksDir = context.dataDir().resolve(BLOCKS_DIR_NAME);
    Path sectionsDir = blocksDir.resolve(SUBPARCELS_DIR_NAME);
    Path entitiesDir = context.dataDir().resolve(ENTITIES_DIR_NAME);
    Path attachmentsDir = context.dataDir().resolve(ATTACHMENTS_DIR_NAME);

    clearDirectory(sectionsDir);
    clearDirectory(entitiesDir);
    clearDirectory(attachmentsDir);
    Files.createDirectories(sectionsDir);

    BlockPalette palette =
        config.usePalette.get()
            ? loadPalette(blocksDir.resolve(PALETTE_FILE_NAME))
            : null;

    BlockPalette finalPalette = palette;
    long[] sectionCount = {0};
    source.forEachBlockSection(
        sectionSize,
        section -> {
          writeSection(config, finalPalette, sectionsDir, section);
          context.progress().report("format_blocks", ++sectionCount[0], "sections");
        });

    if (palette != null) {
      palette.save(blocksDir.resolve(PALETTE_FILE_NAME));
    } else {
      Files.deleteIfExists(blocksDir.resolve(PALETTE_FILE_NAME));
    }

    int[] entityIndex = {0};
    source.forEachEntity(
        entity -> {
          Files.createDirectories(entitiesDir);
          var tag =
              (net.minecraft.nbt.CompoundTag)
                  ParcellaRecordCodecs.ENTITY
                      .encodeStart(NbtOps.INSTANCE, entity)
                      .getOrThrow();
          config.entityDataFormat.get().write(
              entitiesDir.resolve(
                  "%08X%s".formatted(
                      entityIndex[0]++, config.entityDataFormat.get().getSuffix())),
              tag);
          context.progress().report("format_entities", entityIndex[0], "entities");
        });

    int[] attachmentIndex = {0};
    source.forEachAttachment(
        attachment -> {
          Files.createDirectories(attachmentsDir);
          var tag =
              (net.minecraft.nbt.CompoundTag)
                  ParcellaRecordCodecs.ATTACHMENT
                      .encodeStart(NbtOps.INSTANCE, attachment)
                      .getOrThrow();
          NbtFormat.TEXT.write(
              attachmentsDir.resolve("%08X.snbt".formatted(attachmentIndex[0]++)), tag);
          context.progress().report("format_attachments", attachmentIndex[0], "attachments");
        });
  }

  private void writeSection(
      Config config, BlockPalette palette, Path sectionsDir, BlockSection section)
      throws IOException {
    Vec3i coord =
        new Vec3i(
            Math.floorDiv(section.origin().getX(), sectionSize),
            Math.floorDiv(section.origin().getY(), sectionSize),
            Math.floorDiv(section.origin().getZ(), sectionSize));
    long index = ZOrder3D.coordToIndexSigned(coord);
    Path stateFile =
        RadixTreePathGenerator.toPath(
            sectionsDir, index, SUBPARCEL_BLOCK_STATE_SUFFIX);
    Path blockEntityFile =
        RadixTreePathGenerator.toPath(
            sectionsDir, index, SUBPARCEL_BLOCK_ENTITY_SUFFIX);
    Files.createDirectories(stateFile.getParent());

    String encoded =
        switch (config.subparcelFormat.get()) {
          case FLAT -> encodeFlat(section, palette);
          case RLE3D -> encodeRle(section, palette);
        };
    Files.writeString(stateFile, encoded, StandardCharsets.UTF_8);

    if (section.blockEntities().isEmpty()) {
      Files.deleteIfExists(blockEntityFile);
    } else {
      var sorted = new ArrayList<>(section.blockEntities());
      sorted.sort(
          Comparator.comparingInt((io.github.leawind.gitparcel.common.api.parcel.content.BlockEntityRecord e) -> e.pos().getY())
              .thenComparingInt(e -> e.pos().getX())
              .thenComparingInt(e -> e.pos().getZ()));
      var tag =
          (net.minecraft.nbt.CompoundTag)
              ParcellaRecordCodecs.BLOCK_ENTITIES
                  .encodeStart(NbtOps.INSTANCE, new BlockEntities(sorted))
                  .getOrThrow();
      config.blockEntityDataFormat.get().write(blockEntityFile, tag);
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
              if (palette != null) {
                return palette.collect(state);
              }
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

  private static BlockPalette loadPalette(Path path) {
    if (Files.exists(path)) {
      try {
        return BlockPalette.load(path);
      } catch (Exception ignored) {
        // A malformed old palette is replaced; the actual content is still written deterministically.
      }
    }
    return new BlockPalette();
  }

  static void clearDirectory(Path directory) throws IOException {
    NioFileTree.clearDirectory(directory);
  }
}
