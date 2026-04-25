package io.github.leawind.gitparcel.network.protocol.parcelformat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.List;

public record ParcelFormatSpecs(List<ParcelFormat.Spec> savers, List<ParcelFormat.Spec> loaders) {
  public static final Codec<ParcelFormatSpecs> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("savers")
                          .forGetter(ParcelFormatSpecs::savers),
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("loaders")
                          .forGetter(ParcelFormatSpecs::loaders))
                  .apply(inst, ParcelFormatSpecs::new));
}
