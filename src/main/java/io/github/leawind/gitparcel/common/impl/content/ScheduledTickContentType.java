package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.api.parcel.content.ScheduledTickRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Built-in storage for the scheduled block and fluid ticks targeting the parcel extent
 * (SEMANTICS.md rule 6.3).
 *
 * <p>All ticks share one file: a parcel carries at most a few hundred of them, and a single
 * ordered file keeps the format simple. Entries are sorted by (fluid flag, type id, position) so
 * identical tick sets produce byte-identical output (P3), regardless of the world-side chunk
 * iteration order.
 */
public final class ScheduledTickContentType
    implements ParcelContentType<ParcelContentConfig.None> {
  public static final String ID = "scheduled_ticks";
  public static final Spec SPEC = new Spec(ID, 1);
  private static final String TICKS_FILE_NAME = "ticks.snbt";
  private static final Comparator<ScheduledTickRecord> ORDER =
      Comparator.comparing(ScheduledTickRecord::fluid)
          .thenComparing(tick -> tick.typeId().toString())
          .thenComparingInt(tick -> tick.pos().getX())
          .thenComparingInt(tick -> tick.pos().getY())
          .thenComparingInt(tick -> tick.pos().getZ());

  @Override
  public Spec spec() {
    return SPEC;
  }

  @Override
  public Set<String> loadAfter() {
    // Ticks schedule against placed blocks, so they load once the block content has been
    // delivered; the sink still defers world-side scheduling to its commit phase.
    return Set.of(BlockContentType.ID);
  }

  @Override
  public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source)
      throws IOException, ParcelException {
    var output = new ParcelContentFileSupport.ManagedOutput(context.directory());
    List<ScheduledTickRecord> ticks = new ArrayList<>();
    source.forEachScheduledTick(ticks::add);
    if (ticks.isEmpty()) {
      Files.writeString(output.file(ParcelContentFileSupport.EMPTY_DIRECTORY_MARKER), "");
    } else {
      ticks.sort(ORDER);
      NbtFormat.TEXT.write(
          output.file(TICKS_FILE_NAME),
          ParcelRecordCodecs.encode(
              ParcelRecordCodecs.SCHEDULED_TICKS, new ParcelRecordCodecs.ScheduledTicks(ticks)));
      context.progress().report("content_scheduled_ticks", ticks.size(), "ticks");
    }
    output.finish();
  }

  @Override
  public void load(LoadContext<ParcelContentConfig.None> context, ParcelDataSink sink)
      throws IOException, ParcelException {
    Path directory = context.directory();
    Path ticksFile = directory.resolve(TICKS_FILE_NAME);
    Path emptyMarker = directory.resolve(ParcelContentFileSupport.EMPTY_DIRECTORY_MARKER);
    if (Files.isRegularFile(ticksFile)) {
      ParcelContentFileSupport.requireFileSize(
          ticksFile, ParcelContentFileSupport.MAX_RECORD_FILE_BYTES, "Scheduled ticks");
      var ticks =
          ParcelRecordCodecs.decode(
              ParcelRecordCodecs.SCHEDULED_TICKS,
              ParcelContentFileSupport.readTag(NbtFormat.TEXT, ticksFile));
      for (ScheduledTickRecord tick : ticks.entries()) {
        sink.acceptScheduledTick(tick);
      }
      context.progress().report("content_scheduled_ticks", ticks.entries().size(), "ticks");
      ParcelContentFileSupport.validateOwnedFiles(directory, Set.of(ticksFile));
    } else if (Files.isRegularFile(emptyMarker)) {
      ParcelContentFileSupport.validateOwnedFiles(directory, Set.of(emptyMarker));
    } else {
      throw new ParcelException.CorruptedParcelException(
          "Scheduled ticks directory has neither data nor empty marker: " + directory);
    }
  }
}
