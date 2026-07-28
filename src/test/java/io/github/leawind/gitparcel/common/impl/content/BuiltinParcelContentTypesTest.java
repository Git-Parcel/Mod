package io.github.leawind.gitparcel.common.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.LocalAttachmentId;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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

class BuiltinParcelContentTypesTest extends AbstractMinecraftTest {
  private static final Vec3i PARCEL_SIZE = new Vec3i(2, 2, 2);
  @TempDir Path tempDir;

  @Test
  void bothBlockSectionSizesRoundTripAcrossFileSystemProviders() throws Exception {
    for (var size : BlockContentType.BlockSectionSize.values()) {
      var config = new BlockContentType.Config();
      config.sectionSize.set(size);
      Path defaultData = Files.createTempDirectory(tempDir, "default-").resolve("data");
      String defaultDigest = roundTripAt(config, defaultData);
      try (var fs = Jimfs.newFileSystem()) {
        assertEquals(defaultDigest, roundTripAt(config, fs.getPath("/data")));
      }
      Path zip = tempDir.resolve(UUID.randomUUID() + ".zip");
      try (var fs = FileSystems.newFileSystem(zip, Map.of("create", "true"))) {
        assertEquals(defaultDigest, roundTripAt(config, fs.getPath("/data")));
      }
    }
  }

  @Test
  void savingReconcilesEachOwnedDirectory() throws Exception {
    var config = new BlockContentType.Config();
    config.sectionSize.set(BlockContentType.BlockSectionSize.SIZE_16);
    Path data = tempDir.resolve("inherited-data");
    saveAll(data, config);

    Path palette = data.resolve("blocks/palette.txt");
    Files.writeString(palette, Files.readString(palette) + "FF=minecraft:gold_block\n");
    Path staleBlock = data.resolve("blocks/sections/stale.txt");
    Path staleEntity = data.resolve("entities/stale.bin");
    Path staleAttachment = data.resolve("attachments/stale.bin");
    for (Path stale : List.of(staleBlock, staleEntity, staleAttachment)) {
      Files.createDirectories(stale.getParent());
      Files.writeString(stale, "stale");
    }

    saveAll(data, config);

    assertTrue(Files.readString(palette).contains("FF=minecraft:gold_block"));
    assertFalse(Files.exists(staleBlock));
    assertFalse(Files.exists(staleEntity));
    assertFalse(Files.exists(staleAttachment));
    assertRoundTrip(loadAll(data, config));
  }

  @Test
  void contentDirectoriesExistEvenWhenTheyContainNoRecords() throws Exception {
    Path data = tempDir.resolve("empty-data");
    var empty = new EmptySource();

    new EntityContentType().save(saveContext(data.resolve("entities"), null), empty);
    new AttachmentContentType().save(saveContext(data.resolve("attachments"), null), empty);

    assertTrue(Files.isDirectory(data.resolve("entities")));
    assertTrue(Files.isDirectory(data.resolve("attachments")));
    assertTrue(Files.isRegularFile(data.resolve("entities/.empty")));
    assertTrue(Files.isRegularFile(data.resolve("attachments/.empty")));
  }

  private static String roundTripAt(BlockContentType.Config config, Path data) throws Exception {
    Files.createDirectories(data);
    saveAll(data, config);
    assertRoundTrip(loadAll(data, config));
    return digest(data);
  }

  private static void saveAll(Path data, BlockContentType.Config config) throws Exception {
    var source = content();
    new EntityContentType().save(saveContext(data.resolve("entities"), null), source);
    new BlockContentType().save(saveContext(data.resolve("blocks"), config), source);
    new AttachmentContentType().save(saveContext(data.resolve("attachments"), null), source);
  }

  private static CollectingSink loadAll(Path data, BlockContentType.Config config)
      throws Exception {
    var sink = new CollectingSink();
    new AttachmentContentType().load(loadContext(data.resolve("attachments"), null), sink);
    new BlockContentType().load(loadContext(data.resolve("blocks"), config), sink);
    new EntityContentType().load(loadContext(data.resolve("entities"), null), sink);
    return sink;
  }

  private static <C extends io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig<C>>
      ParcelContentType.SaveContext<C> saveContext(Path directory, C config) {
    return new ParcelContentType.SaveContext<>(
        PARCEL_SIZE, Vec3i.ZERO, 0, directory, config, ProgressReporter.NONE);
  }

  private static <C extends io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig<C>>
      ParcelContentType.LoadContext<C> loadContext(Path directory, C config) {
    return new ParcelContentType.LoadContext<>(
        PARCEL_SIZE, Vec3i.ZERO, 0, directory, config, ProgressReporter.NONE);
  }

  private static void assertRoundTrip(CollectingSink sink) {
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
    var section = new BlockSection(BlockPos.ZERO, PARCEL_SIZE, states, List.of());
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
      public void forEachBlockSection(int sectionSize, ParcelDataConsumer<BlockSection> consumer)
          throws IOException, io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(section);
      }

      @Override
      public void forEachEntity(ParcelDataConsumer<EntityRecord> consumer)
          throws IOException, io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(entity);
      }

      @Override
      public void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer)
          throws IOException, io.github.leawind.gitparcel.common.api.exceptions.ParcelException {
        consumer.accept(attachment);
      }
    };
  }

  private static String digest(Path root) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (var paths = Files.walk(root)) {
      for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
        for (Path part : root.relativize(file)) {
          digest.update(part.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
          digest.update((byte) '/');
        }
        digest.update(Files.readAllBytes(file));
      }
    }
    return java.util.HexFormat.of().formatHex(digest.digest());
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

  private static final class EmptySource implements ParcelDataSource {
    @Override
    public void forEachBlockSection(int sectionSize, ParcelDataConsumer<BlockSection> consumer) {}

    @Override
    public void forEachEntity(ParcelDataConsumer<EntityRecord> consumer) {}

    @Override
    public void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer) {}
  }
}
