package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d16.ParcellaD16Writer;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Reader;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.d32.ParcellaD32Writer;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ParcellaTest extends AbstractMinecraftTest {
  @TempDir Path tempDir;

  @Test
  void d16AndD32ShareRoundTripImplementation() throws Exception {
    roundTrip(new ParcellaD16Writer(), new ParcellaD16Reader());
    roundTrip(new ParcellaD32Writer(), new ParcellaD32Reader());
  }

  @Test
  void updatesInheritedComponentTreesAndRemovesStaleFiles() throws Exception {
    var writer = new ParcellaD16Writer();
    var reader = new ParcellaD16Reader();
    var config = new ParcellaFormat.Config();
    Path data = tempDir.resolve("inherited-data");
    var writeContext =
        new ParcelFormat.WriteContext<>(
            new Vec3i(2, 2, 2), Vec3i.ZERO, 0, data, config);
    writer.write(writeContext, content());

    Path palette = data.resolve("blocks/palette.txt");
    Files.writeString(
        palette,
        Files.readString(palette) + "FF=minecraft:gold_block\n");
    Path staleBlock = data.resolve("blocks/sections/stale.txt");
    Path staleEntity = data.resolve("entities/stale.bin");
    Path staleAttachment = data.resolve("attachments/stale.bin");
    Path staleComponent = data.resolve("unknown/value.txt");
    for (Path stale :
        List.of(staleBlock, staleEntity, staleAttachment, staleComponent)) {
      Files.createDirectories(stale.getParent());
      Files.writeString(stale, "stale");
    }

    writer.write(writeContext, content());

    assertTrue(Files.readString(palette).contains("FF=minecraft:gold_block"));
    assertFalse(Files.exists(staleBlock));
    assertFalse(Files.exists(staleEntity));
    assertFalse(Files.exists(staleAttachment));
    assertFalse(Files.exists(staleComponent));
    var sink = new CollectingSink();
    reader.read(
        new ParcelFormat.ReadContext<>(
            writeContext.parcelSize(),
            writeContext.anchor(),
            writeContext.dataVersion(),
            data,
            config),
        sink);
    assertEquals(1, sink.sections.size());
    assertEquals(1, sink.entities.size());
    assertEquals(1, sink.attachments.size());
  }

  private void roundTrip(ParcellaWriter writer, ParcellaReader reader) throws Exception {
    for (boolean palette : List.of(false, true)) {
      for (BlockStateEncoding sectionFormat : BlockStateEncoding.values()) {
        var config = new ParcellaFormat.Config();
        config.usePalette.set(palette);
        config.blockStateEncoding.set(sectionFormat);
        Path defaultData = Files.createTempDirectory(tempDir, "default-").resolve("data");
        String defaultDigest = roundTripAt(writer, reader, config, defaultData);
        try (var fs = Jimfs.newFileSystem()) {
          String jimfsDigest = roundTripAt(writer, reader, config, fs.getPath("/data"));
          assertEquals(defaultDigest, jimfsDigest);
        }
        Path zip = tempDir.resolve(UUID.randomUUID() + ".zip");
        try (var fs = FileSystems.newFileSystem(zip, Map.of("create", "true"))) {
          String zipDigest = roundTripAt(writer, reader, config, fs.getPath("/data"));
          assertEquals(defaultDigest, zipDigest);
        }
      }
    }
  }

  private static String roundTripAt(
      ParcellaWriter writer, ParcellaReader reader, ParcellaFormat.Config config, Path dataDir)
      throws Exception {
    Files.createDirectories(dataDir);
    var context =
        new ParcelFormat.WriteContext<>(
            new Vec3i(2, 2, 2), Vec3i.ZERO, 0, dataDir, config);
    writer.write(context, content());

    var sink = new CollectingSink();
    reader.read(
        new ParcelFormat.ReadContext<>(
            context.parcelSize(),
            context.anchor(),
            context.dataVersion(),
            context.dataDir(),
            config),
        sink);

    assertEquals(1, sink.sections.size());
    assertEquals(
        List.of(
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState()),
        sink.sections.getFirst().states());
    assertEquals(1, sink.entities.size());
    assertEquals(1, sink.attachments.size());
    return digest(dataDir);
  }

  private static String digest(Path root) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (var paths = Files.walk(root)) {
      for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
        Path relative = root.relativize(file);
        for (Path part : relative) {
          digest.update(part.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
          digest.update((byte) '/');
        }
        try (var input = Files.newInputStream(file)) {
          input.transferTo(new java.io.OutputStream() {
            @Override
            public void write(int value) {
              digest.update((byte) value);
            }

            @Override
            public void write(byte[] values, int offset, int length) {
              digest.update(values, offset, length);
            }
          });
        }
      }
    }
    return java.util.HexFormat.of().formatHex(digest.digest());
  }

  private static ParcelDataSource content() {
    var states =
        List.of(
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.AIR.defaultBlockState(),
            Blocks.STONE.defaultBlockState(),
            Blocks.DIRT.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState());
    var section =
        new BlockSection(
            BlockPos.ZERO, new Vec3i(2, 2, 2), states, List.of());
    var entity =
        new EntityRecord(
            Identifier.fromNamespaceAndPath("minecraft", "armor_stand"),
            new Vec3(0.5, 1, 0.5),
            BlockPos.ZERO,
            new CompoundTag(),
            List.of());
    CompoundTag payload = new CompoundTag();
    payload.putString("value", "opaque");
    var attachment =
        new AttachmentRecord(
            new LocalAttachmentId("test-0"),
            Identifier.fromNamespaceAndPath("test", "opaque"),
            3,
            false,
            payload);
    return new ParcelDataSource() {
      @Override
      public void forEachBlockSection(
          int sectionSize,
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer<BlockSection>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(section);
      }

      @Override
      public void forEachEntity(
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer<EntityRecord>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(entity);
      }

      @Override
      public void forEachAttachment(
          io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer<
                  AttachmentRecord>
              consumer)
          throws java.io.IOException,
              io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(attachment);
      }
    };
  }

  private static final class CollectingSink implements ParcelDataSink {
    final List<BlockSection> sections = new ArrayList<>();
    final List<EntityRecord> entities = new ArrayList<>();
    final List<AttachmentRecord> attachments = new ArrayList<>();

    @Override
    public void acceptBlockSection(BlockSection section) {
      sections.add(section);
    }

    @Override
    public void acceptEntity(EntityRecord entity) {
      entities.add(entity);
    }

    @Override
    public void acceptAttachment(AttachmentRecord attachment) {
      attachments.add(attachment);
    }
  }
}
