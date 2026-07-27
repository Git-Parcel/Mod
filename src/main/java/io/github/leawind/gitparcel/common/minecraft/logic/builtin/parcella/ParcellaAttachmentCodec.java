package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import static io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.ATTACHMENTS_DIR_NAME;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponent;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponents;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella.ParcellaFormat.Config;
import java.io.IOException;
import java.nio.file.Path;

final class ParcellaAttachmentCodec implements ParcellaComponentCodec {
  @Override
  public ParcelDataComponent component() {
    return ParcelDataComponents.ATTACHMENTS;
  }

  @Override
  public String directoryName() {
    return ATTACHMENTS_DIR_NAME;
  }

  @Override
  public void write(
      ParcelFormat.WriteContext<Config> context, ParcelDataSource source, Path directory)
      throws IOException, ParcelException {
    var output = new ParcellaCodecSupport.ManagedOutput(directory);
    int[] index = {0};
    source.forEachAttachment(
        attachment -> {
          Path path = output.file("%08X.snbt".formatted(index[0]++));
          NbtFormat.TEXT.write(
              path,
              ParcellaRecordCodecs.encode(ParcellaRecordCodecs.ATTACHMENT, attachment));
          context.progress().report("format_attachments", index[0], "attachments");
        });
    output.finish();
  }

  @Override
  public void read(
      ParcelFormat.ReadContext<Config> context, ParcelDataSink sink, Path directory)
      throws IOException, ParcelException {
    long[] count = {0};
    ParcellaCodecSupport.readRecordDirectory(
        directory,
        ".snbt",
        path -> {
          sink.acceptAttachment(
              ParcellaRecordCodecs.decode(
                  ParcellaRecordCodecs.ATTACHMENT,
                  ParcellaCodecSupport.readTag(NbtFormat.TEXT, path)));
          context.progress().report("format_attachments", ++count[0], "attachments");
        });
  }
}
