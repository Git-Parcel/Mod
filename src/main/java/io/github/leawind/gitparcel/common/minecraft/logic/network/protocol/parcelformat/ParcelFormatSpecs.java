package io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  public Set<ParcelFormat.Spec> toSet() {
    Set<ParcelFormat.Spec> set = new HashSet<>();
    set.addAll(savers());
    set.addAll(loaders());
    return set;
  }

  public boolean hasSaver(ParcelFormat.Spec spec) {
    return savers().contains(spec);
  }

  public boolean hasLoader(ParcelFormat.Spec spec) {
    return loaders().contains(spec);
  }

  public static ParcelFormatSpecs empty() {
    return new ParcelFormatSpecs(List.of(), List.of());
  }
}
