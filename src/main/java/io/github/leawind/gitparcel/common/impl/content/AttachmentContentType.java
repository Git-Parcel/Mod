package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;

/** Built-in storage and streaming behavior for operation-scoped attachments. */
public final class AttachmentContentType implements ParcelContentType<ParcelContentConfig.None> {
  public static final String ID = "attachments";
  public static final Spec SPEC = new Spec(ID, 1);

  @Override
  public Spec spec() {
    return SPEC;
  }

  @Override
  public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source)
      throws IOException, ParcelException {
    var output = new ParcelContentFileSupport.ManagedOutput(context.directory());
    int[] index = {0};
    source.forEachAttachment(
        attachment -> {
          var path = output.file("%08X.snbt".formatted(index[0]++));
          NbtFormat.TEXT.write(
              path,
              ParcelRecordCodecs.encode(ParcelRecordCodecs.ATTACHMENT, attachment));
          context.progress().report("content_attachments", index[0], "attachments");
        });
    output.finish();
  }

  @Override
  public void load(LoadContext<ParcelContentConfig.None> context, ParcelDataSink sink)
      throws IOException, ParcelException {
    long[] count = {0};
    ParcelContentFileSupport.readRecordDirectory(
        context.directory(),
        ".snbt",
        path -> {
          sink.acceptAttachment(
              ParcelRecordCodecs.decode(
                  ParcelRecordCodecs.ATTACHMENT,
                  ParcelContentFileSupport.readTag(NbtFormat.TEXT, path)));
          context.progress().report("content_attachments", ++count[0], "attachments");
        });
  }
}
