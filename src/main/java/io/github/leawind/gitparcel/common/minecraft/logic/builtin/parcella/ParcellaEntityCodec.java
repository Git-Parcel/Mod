package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.ENTITIES_DIR_NAME;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponent;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponents;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.Config;
import java.io.IOException;
import java.nio.file.Path;

final class ParcellaEntityCodec implements ParcellaComponentCodec {
  @Override
  public ParcelDataComponent component() {
    return ParcelDataComponents.ENTITIES;
  }

  @Override
  public String directoryName() {
    return ENTITIES_DIR_NAME;
  }

  @Override
  public void write(
      ParcelFormat.WriteContext<Config> context, ParcelDataSource source, Path directory)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    NbtFormat format = config.entityDataFormat.get();
    var output = new ParcellaCodecSupport.ManagedOutput(directory);
    int[] index = {0};
    source.forEachEntity(
        entity -> {
          Path path = output.file("%08X%s".formatted(index[0]++, format.getSuffix()));
          format.write(path, ParcellaRecordCodecs.encode(ParcellaRecordCodecs.ENTITY, entity));
          context.progress().report("format_entities", index[0], "entities");
        });
    output.finish();
  }

  @Override
  public void read(
      ParcelFormat.ReadContext<Config> context, ParcelDataSink sink, Path directory)
      throws IOException, ParcelException {
    Config config = context.config() == null ? new Config() : context.config();
    NbtFormat format = config.entityDataFormat.get();
    long[] count = {0};
    ParcellaCodecSupport.readRecordDirectory(
        directory,
        format.getSuffix(),
        path -> {
          sink.acceptEntity(
              ParcellaRecordCodecs.decode(
                  ParcellaRecordCodecs.ENTITY,
                  ParcellaCodecSupport.readTag(format, path)));
          context.progress().report("format_entities", ++count[0], "entities");
        });
  }
}
