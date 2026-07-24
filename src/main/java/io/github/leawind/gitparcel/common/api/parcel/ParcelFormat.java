package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.regex.Pattern;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public sealed interface ParcelFormat permits ParcelFormat.Impl {

  record Spec(String id, int version) implements Comparable<Spec> {
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

    @Override
    public int compareTo(ParcelFormat.@NonNull Spec o) {
      int idComparison = this.id.compareTo(o.id);
      if (idComparison != 0) {
        return idComparison;
      }
      return Integer.compare(this.version, o.version);
    }
  }

  Spec spec();

  enum Feature {
    ROTATE,
    MIRROR,
  }

  default EnumSet<Feature> features() {
    return EnumSet.noneOf(Feature.class);
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
     * Writes parcel content described by an operation context.
     *
     * <p>Legacy {@link Saver} implementations continue to override the parameter-list overload.
     * New implementations should implement {@link ContextSaver}, which makes this method the
     * required entry point.
     */
    default void save(SaveContext<C> context)
        throws IOException, ParcelException.UnsupportedFeature {
      save(
          context.level(),
          context.parcelSize(),
          context.anchor(),
          context.transform(),
          context.dataDir(),
          context.ignoreEntities(),
          context.config());
    }

    /**
     * Writes parcel content using the legacy parameter-list contract.
     *
     * @apiNote If the format does not support features like rotation, mirror, but the given
     *     transform does, {@link ParcelException.UnsupportedFeature} will be thrown.
     * @deprecated Implement {@link ContextSaver} and override {@link #save(SaveContext)}. This
     *     compatibility overload remains available while third-party formats migrate.
     */
    @Deprecated(forRemoval = false)
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

  /**
   * Context-based saver contract for new and migrated format implementations.
   *
   * <p>The legacy overload is implemented as an adapter, preserving callers that still use the old
   * signature without allowing an implementation to accidentally inherit mutually recursive
   * defaults.
   */
  interface ContextSaver<C extends ParcelFormatConfig<C>> extends Saver<C> {
    @Override
    void save(SaveContext<C> context) throws IOException, ParcelException.UnsupportedFeature;

    @Override
    @Deprecated(forRemoval = false)
    default void save(
        Level level,
        Vec3i parcelSize,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config)
        throws IOException, ParcelException.UnsupportedFeature {
      save(
          new SaveContext<>(
              level, parcelSize, transform, anchor, dataDir, ignoreEntities, config));
    }
  }

  interface Loader<C extends ParcelFormatConfig<C>> extends Impl<C> {

    /**
     * Reads parcel content from disk and places it into the target game level.
     *
     * <p>Legacy {@link Loader} implementations continue to override the parameter-list overload.
     * New implementations should implement {@link ContextLoader}, which makes this method the
     * required entry point.
     */
    default void load(LoadContext<C> context)
        throws IOException, ParcelException.CorruptedParcelException {
      load(
          context.level(),
          context.parcelSize(),
          context.anchor(),
          context.transform(),
          context.dataDir(),
          context.ignoreBlocks(),
          context.ignoreEntities(),
          context.blockUpdateFlags(),
          context.config());
    }

    /**
     * Reads parcel content from disk and places it into the target game level using the legacy
     * parameter-list contract.
     *
     * @param transform Transformation to apply when placing the parcel in the world
     * @param dataDir Directory containing the format-specific parcel data
     * @param ignoreBlocks When true, blocks will not be placed into the world. Not guaranteed to be
     *     supported by all formats.
     * @param ignoreEntities When true, entities will not be spawned into the world. Guaranteed to
     *     be supported by all formats.
     * @param flags Block update flags to use when placing blocks
     * @deprecated Implement {@link ContextLoader} and override {@link #load(LoadContext)}. This
     *     compatibility overload remains available while third-party formats migrate.
     */
    @Deprecated(forRemoval = false)
    void load(
        ServerLevelAccessor level,
        Vec3i size,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        int flags,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException;
  }

  /**
   * Context-based loader contract for new and migrated format implementations.
   *
   * <p>The legacy overload is implemented as an adapter, preserving callers that still use the old
   * signature without allowing an implementation to accidentally inherit mutually recursive
   * defaults.
   */
  interface ContextLoader<C extends ParcelFormatConfig<C>> extends Loader<C> {
    @Override
    void load(LoadContext<C> context)
        throws IOException, ParcelException.CorruptedParcelException;

    @Override
    @Deprecated(forRemoval = false)
    default void load(
        ServerLevelAccessor level,
        Vec3i size,
        Vec3i anchor,
        ParcelTransform transform,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        int flags,
        @Nullable C config)
        throws IOException, ParcelException.CorruptedParcelException {
      load(
          new LoadContext<>(
              level,
              size,
              transform,
              anchor,
              dataDir,
              ignoreBlocks,
              ignoreEntities,
              flags,
              config));
    }
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

    public Vec3i parcelSize() {
      return parcelSize;
    }

    public ParcelTransform transform() {
      return transform;
    }

    public Path dataDir() {
      return dataDir;
    }

    public Vec3i anchor() {
      return anchor;
    }
  }

  class SaveContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    /** @deprecated Use {@link #level()} to access the concrete saving level. */
    @Deprecated(forRemoval = false)
    public final LevelAccessor level;

    private final Level savingLevel;
    public final boolean ignoreEntities;
    public final @Nullable C config;

    public SaveContext(
        Level level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreEntities,
        @Nullable C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.savingLevel = level;
      this.ignoreEntities = ignoreEntities;
      this.config = config;
    }

    public Level level() {
      return savingLevel;
    }

    public boolean ignoreEntities() {
      return ignoreEntities;
    }

    public @Nullable C config() {
      return config;
    }
  }

  class LoadContext<C extends ParcelFormatConfig<C>> extends BaseContext {
    public final ServerLevelAccessor level;
    public final boolean ignoreBlocks;
    public final boolean ignoreEntities;
    public final int flags;
    public final @Nullable C config;

    public LoadContext(
        ServerLevelAccessor level,
        Vec3i parcelSize,
        ParcelTransform transform,
        Vec3i anchor,
        Path dataDir,
        boolean ignoreBlocks,
        boolean ignoreEntities,
        int flags,
        @Nullable C config) {
      super(parcelSize, transform, dataDir, anchor);
      this.level = level;
      this.ignoreBlocks = ignoreBlocks;
      this.ignoreEntities = ignoreEntities;
      this.flags = flags;
      this.config = config;
    }

    public ServerLevelAccessor level() {
      return level;
    }

    public boolean ignoreBlocks() {
      return ignoreBlocks;
    }

    public boolean ignoreEntities() {
      return ignoreEntities;
    }

    public int blockUpdateFlags() {
      return flags;
    }

    public @Nullable C config() {
      return config;
    }
  }
}
