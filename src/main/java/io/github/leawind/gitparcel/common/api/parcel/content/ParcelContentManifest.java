package io.github.leawind.gitparcel.common.api.parcel.content;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

/** Version and optional configuration recorded for one content directory in a snapshot. */
public record ParcelContentManifest(int version, @Nullable JsonElement config) {
  public static final Codec<ParcelContentManifest> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.INT.fieldOf("version").forGetter(ParcelContentManifest::version),
                      ExtraCodecs.JSON
                          .optionalFieldOf("config")
                          .forGetter(value -> Optional.ofNullable(value.config)))
                  .apply(instance, ParcelContentManifest::new));

  public ParcelContentManifest {
    if (version < 0) {
      throw new IllegalArgumentException("Parcel content version must be non-negative");
    }
    config = config == null ? null : config.deepCopy();
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private ParcelContentManifest(int version, Optional<JsonElement> config) {
    this(version, config.orElse(null));
  }

  @Override
  public @Nullable JsonElement config() {
    return config == null ? null : config.deepCopy();
  }

  public ParcelContentType.Spec spec(String id) {
    return new ParcelContentType.Spec(id, version);
  }
}
