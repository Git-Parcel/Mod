package io.github.leawind.gitparcel.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.regex.Pattern;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public sealed interface ParcelFormat permits ParcelFormat.Impl {

  Spec spec();

  default EnumSet<Feature> features() {
    return EnumSet.noneOf(Feature.class);
  }

  default String id() {
    return spec().id();
  }

  default int version() {
    return spec().version();
  }

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

    @NonNull
    @Override
    public String toString() {
      return String.format("%s:%d", id, version);
    }
  }

  enum Feature {
    ROTATE,
    MIRROR,
  }

  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat {

    default @NonNull <T> C castConfig(@NonNull T config) throws ClassCastException {
      var clazz = configClass();
      if (clazz == null) {
        throw new ClassCastException(
            String.format("Expected null, got %s: %s", config.getClass().getSimpleName(), config));
      }
      return clazz.cast(config);
    }

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
        Level level,
        Vec3i parcelSize,
        Vec3i anchor,
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
     * @param flags Block update flags to use when placing blocks. Usually {@code
     *     Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE | Block.UPDATE_KNOWN_SHAPE |
     *     Block.UPDATE_SKIP_ALL_SIDEEFFECTS}
     */
    void load(
        ServerLevelAccessor level,
        Vec3i size,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException;
  }

  class BaseContext {
    public final Vec3i parcelSize;
    public final ParcelTransform transform;
    public final Path dataDir;
    public final Vec3i anchor;

    public BaseContext(Vec3i parcelSize, ParcelTransform transform, Path dataDir, Vec3i anchor) {
      this.parcelSize = parcelSize;
      this.transform = transform;
      this.dataDir = dataDir;
      this.anchor = anchor;
    }
  }

  class SaveContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final LevelAccessor level;
    public final boolean ignoreEntities;
    public final C config;

    public SaveContext(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreEntities,
        C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }
  }

  class LoadContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final ServerLevelAccessor level;
    public final boolean ignoreBlocks;
    public final boolean ignoreEntities;
    public final @Block.UpdateFlags int flags;
    public final C config;

    public LoadContext(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        @Block.UpdateFlags int flags,
        C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.ignoreBlocks = ignoreBlocks;
      this.ignoreEntities = ignoreEntities;
      this.flags = flags;
      this.config = config;
    }
  }
}
