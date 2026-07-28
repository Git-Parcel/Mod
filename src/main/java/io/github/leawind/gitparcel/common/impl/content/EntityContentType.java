package io.github.leawind.gitparcel.common.impl.content;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentConfig;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Built-in storage and streaming behavior for parcel entities. */
public final class EntityContentType implements ParcelContentType<ParcelContentConfig.None> {
  public static final String ID = "entities";
  public static final Spec SPEC = new Spec(ID, 1);

  @Override
  public Spec spec() {
    return SPEC;
  }

  @Override
  public Set<String> loadAfter() {
    return Set.of(AttachmentContentType.ID);
  }

  @Override
  public void save(SaveContext<ParcelContentConfig.None> context, ParcelDataSource source)
      throws IOException, ParcelException {
    var output = new ParcelContentFileSupport.ManagedOutput(context.directory());
    int[] index = {0};
    source.forEachEntity(
        entity -> {
          Path path = output.file("%08X.snbt".formatted(index[0]++));
          NbtFormat.TEXT.write(
              path, ParcelRecordCodecs.encode(ParcelRecordCodecs.ENTITY, entity));
          context.progress().report("content_entities", index[0], "entities");
        });
    if (index[0] == 0) {
      Files.writeString(output.file(ParcelContentFileSupport.EMPTY_DIRECTORY_MARKER), "");
    }
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
          sink.acceptEntity(
              ParcelRecordCodecs.decode(
                  ParcelRecordCodecs.ENTITY,
                  ParcelContentFileSupport.readTag(NbtFormat.TEXT, path)));
          context.progress().report("content_entities", ++count[0], "entities");
        });
  }
}
