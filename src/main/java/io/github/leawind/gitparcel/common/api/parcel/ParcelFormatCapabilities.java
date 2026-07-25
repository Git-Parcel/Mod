package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The parcel formats that a server can write and read. */
public record ParcelFormatCapabilities(
    List<ParcelFormat.Spec> writers, List<ParcelFormat.Spec> readers) {
  public static final Codec<ParcelFormatCapabilities> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("writers")
                          .forGetter(ParcelFormatCapabilities::writers),
                      ParcelFormat.Spec.CODEC
                          .listOf()
                          .fieldOf("readers")
                          .forGetter(ParcelFormatCapabilities::readers))
                  .apply(inst, ParcelFormatCapabilities::new));

  public ParcelFormatCapabilities {
    writers = List.copyOf(writers);
    readers = List.copyOf(readers);
  }

  public Set<ParcelFormat.Spec> toSet() {
    Set<ParcelFormat.Spec> set = new HashSet<>();
    set.addAll(writers);
    set.addAll(readers);
    return Set.copyOf(set);
  }

  public boolean hasWriter(ParcelFormat.Spec spec) {
    return writers.contains(spec);
  }

  public boolean hasReader(ParcelFormat.Spec spec) {
    return readers.contains(spec);
  }

  public static ParcelFormatCapabilities from(ParcelFormatRegistry registry) {
    var writers = registry.streamWriters().map(ParcelFormat::spec).toList();
    var readers = registry.streamReaders().map(ParcelFormat::spec).toList();
    return new ParcelFormatCapabilities(writers, readers);
  }

  public static ParcelFormatCapabilities empty() {
    return new ParcelFormatCapabilities(List.of(), List.of());
  }
}
