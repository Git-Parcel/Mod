package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The parcel formats that a server can save and load. */
public record ParcelFormatCapabilities(
    List<ParcelFormat.Spec> savers, List<ParcelFormat.Spec> loaders) {
  public static final Codec<ParcelFormatCapabilities> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("savers")
                          .forGetter(ParcelFormatCapabilities::savers),
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("loaders")
                          .forGetter(ParcelFormatCapabilities::loaders))
                  .apply(inst, ParcelFormatCapabilities::new));

  public ParcelFormatCapabilities {
    savers = List.copyOf(savers);
    loaders = List.copyOf(loaders);
  }

  public Set<ParcelFormat.Spec> toSet() {
    Set<ParcelFormat.Spec> set = new HashSet<>();
    set.addAll(savers);
    set.addAll(loaders);
    return Set.copyOf(set);
  }

  public boolean hasSaver(ParcelFormat.Spec spec) {
    return savers.contains(spec);
  }

  public boolean hasLoader(ParcelFormat.Spec spec) {
    return loaders.contains(spec);
  }

  public static ParcelFormatCapabilities from(ParcelFormatRegistry registry) {
    var savers = registry.streamSavers().map(ParcelFormat::spec).toList();
    var loaders = registry.streamLoaders().map(ParcelFormat::spec).toList();
    return new ParcelFormatCapabilities(savers, loaders);
  }

  public static ParcelFormatCapabilities empty() {
    return new ParcelFormatCapabilities(List.of(), List.of());
  }
}
