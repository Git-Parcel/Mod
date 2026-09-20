package io.github.leawind.gitparcel.common.impl.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import io.github.leawind.gitparcel.common.api.parcel.content.AttachmentRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.BlockSection;
import io.github.leawind.gitparcel.common.api.parcel.content.EntityRecord;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataConsumer;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.api.parcel.content.ScheduledTickRecord;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.ticks.TickPriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScheduledTickContentTypeTest extends AbstractMinecraftTest {
  private static final Vec3i PARCEL_SIZE = new Vec3i(4, 4, 4);
  @TempDir Path tempDir;

  @Test
  void roundTripsAcrossFileSystemProviders() throws Exception {
    List<ScheduledTickRecord> ticks =
        List.of(
            tick(false, "redstone_block", new BlockPos(1, 2, 3), 40),
            tick(false, "redstone_lamp", new BlockPos(0, 0, 0), -5, TickPriority.EXTREMELY_HIGH),
            tick(true, "water", new BlockPos(2, 1, 0), 7),
            tick(true, "flowing_water", new BlockPos(2, 1, 1), 0));
    Path defaultData = Files.createTempDirectory(tempDir, "default-").resolve("data");
    String defaultDigest = roundTripAt(ticks, defaultData);
    try (var fs = Jimfs.newFileSystem(Configuration.unix())) {
      assertEquals(defaultDigest, roundTripAt(ticks, fs.getPath("/data")));
    }
    Path zip = tempDir.resolve(java.util.UUID.randomUUID() + ".zip");
    try (var fs = FileSystems.newFileSystem(zip, Map.of("create", "true"))) {
      assertEquals(defaultDigest, roundTripAt(ticks, fs.getPath("/data")));
    }
  }

  /** Byte-identical output regardless of the enumeration order the source produced (P3). */
  @Test
  void sortsRegardlessOfInputOrder() throws Exception {
    List<ScheduledTickRecord> ticks =
        List.of(
            tick(true, "water", new BlockPos(2, 1, 0), 7),
            tick(false, "redstone_block", new BlockPos(1, 2, 3), 40),
            tick(true, "flowing_water", new BlockPos(2, 1, 1), 0),
            tick(false, "redstone_lamp", new BlockPos(0, 0, 0), -5));
    Path forward = Files.createTempDirectory(tempDir, "forward-");
    Path reversed = Files.createTempDirectory(tempDir, "reversed-");
    saveTicks(ticks, forward.resolve("data"));
    saveTicks(ticks.reversed(), reversed.resolve("data"));
    assertEquals(
        Files.readString(forward.resolve("data/scheduled_ticks/ticks.snbt")),
        Files.readString(reversed.resolve("data/scheduled_ticks/ticks.snbt")));
  }

  @Test
  void emptySourceWritesMarker() throws Exception {
    Path data = tempDir.resolve("empty-data");
    saveTicks(List.of(), data);

    assertTrue(
        Files.isRegularFile(data.resolve("scheduled_ticks").resolve(ParcelContentFileSupport.EMPTY_DIRECTORY_MARKER)));

    var sink = new CollectingSink();
    new ScheduledTickContentType().load(loadContext(data.resolve("scheduled_ticks")), sink);
    assertTrue(sink.ticks.isEmpty());
  }

  @Test
  void rejectsDirectoryWithoutDataOrMarker() {
    Path directory = tempDir.resolve("orphan");
    assertThrows(
        ParcelException.CorruptedParcelException.class,
        () -> new ScheduledTickContentType().load(loadContext(directory), new CollectingSink()));
  }

  /** A save over an inherited file replaces it when ticks disappear (owned-directory rule). */
  @Test
  void savingReconcilesOwnedDirectory() throws Exception {
    Path data = tempDir.resolve("reconcile");
    saveTicks(
        List.of(tick(false, "redstone_block", new BlockPos(1, 2, 3), 40)), data);
    assertTrue(Files.isRegularFile(data.resolve("scheduled_ticks/ticks.snbt")));

    saveTicks(List.of(), data);
    assertTrue(
        Files.notExists(data.resolve("scheduled_ticks/ticks.snbt")));
    assertTrue(
        Files.isRegularFile(data.resolve("scheduled_ticks").resolve(ParcelContentFileSupport.EMPTY_DIRECTORY_MARKER)));
  }

  private String roundTripAt(List<ScheduledTickRecord> ticks, Path data) throws Exception {
    Files.createDirectories(data);
    saveTicks(ticks, data);
    var sink = new CollectingSink();
    new ScheduledTickContentType().load(loadContext(data.resolve("scheduled_ticks")), sink);
    assertEquals(ticks.stream().sorted(order()).toList(), sink.ticks);
    return digest(data);
  }

  private static java.util.Comparator<ScheduledTickRecord> order() {
    return java.util.Comparator.comparing(ScheduledTickRecord::fluid)
        .thenComparing(tick -> tick.typeId().toString())
        .thenComparingInt(tick -> tick.pos().getX())
        .thenComparingInt(tick -> tick.pos().getY())
        .thenComparingInt(tick -> tick.pos().getZ());
  }

  private void saveTicks(List<ScheduledTickRecord> ticks, Path data) throws Exception {
    Files.createDirectories(data);
    new ScheduledTickContentType()
        .save(
            saveContext(data.resolve("scheduled_ticks")),
            new TickSource(ticks));
  }

  private static ScheduledTickRecord tick(
      boolean fluid, String type, BlockPos pos, int delay, TickPriority priority) {
    return new ScheduledTickRecord(
        fluid, Identifier.fromNamespaceAndPath("minecraft", type), pos, delay, priority);
  }

  private static ScheduledTickRecord tick(boolean fluid, String type, BlockPos pos, int delay) {
    return tick(fluid, type, pos, delay, TickPriority.NORMAL);
  }

  private static <C extends ParcelContentConfig<C>>
      ParcelContentType.SaveContext<C> saveContext(Path directory) {
    return new ParcelContentType.SaveContext<>(
        PARCEL_SIZE, Vec3i.ZERO, 0, directory, null, ProgressReporter.NONE);
  }

  private static <C extends ParcelContentConfig<C>>
      ParcelContentType.LoadContext<C> loadContext(Path directory) {
    return new ParcelContentType.LoadContext<>(
        PARCEL_SIZE, Vec3i.ZERO, 0, directory, null, ProgressReporter.NONE);
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

  private record TickSource(List<ScheduledTickRecord> ticks) implements ParcelDataSource {
    @Override
    public void forEachBlockSection(int sectionSize, ParcelDataConsumer<BlockSection> consumer)
        throws IOException, ParcelException {}

    @Override
    public void forEachEntity(ParcelDataConsumer<EntityRecord> consumer)
        throws IOException, ParcelException {}

    @Override
    public void forEachAttachment(ParcelDataConsumer<AttachmentRecord> consumer)
        throws IOException, ParcelException {}

    @Override
    public void forEachScheduledTick(ParcelDataConsumer<ScheduledTickRecord> consumer)
        throws IOException, ParcelException {
      for (ScheduledTickRecord tick : ticks) {
        consumer.accept(tick);
      }
    }
  }

  private static final class CollectingSink implements ParcelDataSink {
    final List<ScheduledTickRecord> ticks = new ArrayList<>();

    @Override
    public void acceptScheduledTick(ScheduledTickRecord tick) {
      ticks.add(tick);
    }
  }
}
