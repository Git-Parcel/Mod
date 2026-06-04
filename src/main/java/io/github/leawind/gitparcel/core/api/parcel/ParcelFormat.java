package io.github.leawind.gitparcel.core.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.core.api.error.ParcelException;
import io.github.leawind.gitparcel.core.api.parcel.config.ParcelFormatConfig;
import io.github.leawind.gitparcel.core.bridge.level.VoxelSource;
import io.github.leawind.gitparcel.core.bridge.level.VoxelTarget;
import io.github.leawind.gitparcel.core.bridge.math.MathBridge;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public sealed interface ParcelFormat permits ParcelFormat.Impl {

  record Spec(String id, int version) {
    public static final Codec<Spec> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.fieldOf("id").forGetter(Spec::id),
                        Codec.INT.fieldOf("version").forGetter(Spec::version))
                    .apply(inst, Spec::new));

    public static final Pattern ID_PATTERN =
        Pattern.compile("^[a-zA-Z_\\-]([a-zA-Z_\\-0-9]+){0,63}$");

    public Spec {
      if (!ID_PATTERN.matcher(id).matches()) {
        throw new IllegalArgumentException("ID must match " + ID_PATTERN);
      }
    }

    @Override
    public @NonNull String toString() {
      return String.format("%s:%d", id, version);
    }
  }

  Spec spec();

  enum Feature {
    ROTATE,
    MIRROR
  }

  EnumSet<Feature> features();

  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat {
    default @Nullable Class<C> configClass() {
      return null;
    }

    default @Nullable C getDefaultConfig() {
      return null;
    }
  }

  interface Saver<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * @apiNote If the format does not support features like rotation, mirror, but the given
     *     transform does, {@link ParcelException.UnsupportedFeature} will be thrown.
     */
    void save(
        VoxelSource level,
        MathBridge.Vec3i parcelSize,
        MathBridge.Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException, ParcelException.UnsupportedFeature;
  }

  interface Loader<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Reads parcel content from disk and places it into the target game level.
     *
     * @param transform Transformation to apply when placing the parcel in the world
     * @param dataDir Directory containing the format-specific parcel data
     * @param ignoreBlocks When true, blocks will not be placed into the world. Not guaranteed to be
     *     supported by all formats.
     * @param ignoreEntities When true, entities will not be spawned into the world. Guaranteed to
     *     be supported by all formats.
     */
    void load(
        VoxelTarget level,
        MathBridge.Vec3i size,
        MathBridge.Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException;
  }
}
