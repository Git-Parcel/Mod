package io.github.leawind.gitparcel.common.minecraft.logic.builtin.parcella;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataComponent;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelDataSource;
import java.io.IOException;
import java.nio.file.Path;

/** Parcella-specific encoding of one domain-level parcel data component. */
interface ParcellaComponentCodec {
  ParcelDataComponent component();

  String directoryName();

  void write(
      ParcelFormat.WriteContext<ParcellaFormat.Config> context,
      ParcelDataSource source,
      Path directory)
      throws IOException, ParcelException;

  void read(
      ParcelFormat.ReadContext<ParcellaFormat.Config> context,
      ParcelDataSink sink,
      Path directory)
      throws IOException, ParcelException;
}
