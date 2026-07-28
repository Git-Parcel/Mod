package io.github.leawind.gitparcel.common.api.parcel.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/** Parcel content implementations available on a server and the newest active version per id. */
public record ParcelContentCapabilities(
    List<ParcelContentType.Spec> registered, List<ParcelContentType.Spec> active) {
  public static final Codec<ParcelContentCapabilities> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelContentType.Spec.CODEC
                          .listOf()
                          .fieldOf("registered")
                          .forGetter(ParcelContentCapabilities::registered),
                      ParcelContentType.Spec.CODEC
                          .listOf()
                          .fieldOf("active")
                          .forGetter(ParcelContentCapabilities::active))
                  .apply(inst, ParcelContentCapabilities::new));

  public ParcelContentCapabilities {
    registered = registered.stream().sorted().toList();
    active = active.stream().sorted().toList();
  }

  public boolean isActive(ParcelContentType.Spec spec) {
    return active.contains(spec);
  }

  public static ParcelContentCapabilities from(ParcelContentTypeRegistry registry) {
    var registered = registry.registeredTypes().stream().map(ParcelContentType::spec).toList();
    var active = registry.latestTypes().stream().map(ParcelContentType::spec).toList();
    return new ParcelContentCapabilities(registered, active);
  }

  public static ParcelContentCapabilities empty() {
    return new ParcelContentCapabilities(List.of(), List.of());
  }
}
