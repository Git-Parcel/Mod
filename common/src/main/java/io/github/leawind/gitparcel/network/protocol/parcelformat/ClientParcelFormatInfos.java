package io.github.leawind.gitparcel.network.protocol.parcelformat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.List;

public record ClientParcelFormatInfos(
    List<ParcelFormat.Info> savers, List<ParcelFormat.Info> loaders) {
  public static final Codec<ClientParcelFormatInfos> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Info.CODEC
                          .listOf()
                          .fieldOf("savers")
                          .forGetter(ClientParcelFormatInfos::savers),
                      ParcelFormat.Info.CODEC
                          .listOf()
                          .fieldOf("loaders")
                          .forGetter(ClientParcelFormatInfos::loaders))
                  .apply(inst, ClientParcelFormatInfos::new));
}
