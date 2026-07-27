package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Shared streaming reader for all Parcella grid sizes. */
public abstract class ParcellaReader
    implements ParcelFormat.Reader<ParcellaFormat.Config>, ParcellaFormat {
  private final List<ParcellaComponentCodec> components;

  protected ParcellaReader(int sectionSize, ParcellaDigitCodec digitCodec) {
    components =
        List.of(
            new ParcellaAttachmentCodec(),
            new ParcellaBlockCodec(sectionSize, digitCodec),
            new ParcellaEntityCodec());
  }

  @Override
  public void read(ReadContext<Config> context, ParcelDataSink sink)
      throws IOException, ParcelException {
    ParcellaCodecSupport.validateDataRoot(
        context.dataDir(),
        components.stream()
            .map(ParcellaComponentCodec::directoryName)
            .collect(Collectors.toUnmodifiableSet()));
    for (ParcellaComponentCodec component : components) {
      component.read(
          context, sink, context.dataDir().resolve(component.directoryName()));
    }
    sink.finish();
  }
}
