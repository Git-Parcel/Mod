package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Shared streaming writer for all Parcella grid sizes. */
public abstract class ParcellaWriter
    implements ParcelFormat.Writer<ParcellaFormat.Config>, ParcellaFormat {
  private final int sectionSize;
  private final List<ParcellaComponentCodec> components;

  protected ParcellaWriter(int sectionSize, ParcellaDigitCodec digitCodec) {
    this.sectionSize = sectionSize;
    this.components =
        List.of(
            new ParcellaBlockCodec(sectionSize, digitCodec),
            new ParcellaEntityCodec(),
            new ParcellaAttachmentCodec());
  }

  @Override
  public int blockSectionSize() {
    return sectionSize;
  }

  @Override
  public void write(WriteContext<Config> context, ParcelDataSource source)
      throws IOException, ParcelException {
    for (ParcellaComponentCodec component : components) {
      component.write(
          context, source, context.dataDir().resolve(component.directoryName()));
    }
    ParcellaCodecSupport.reconcileDataRoot(
        context.dataDir(),
        components.stream()
            .map(ParcellaComponentCodec::directoryName)
            .collect(Collectors.toUnmodifiableSet()));
  }
}
