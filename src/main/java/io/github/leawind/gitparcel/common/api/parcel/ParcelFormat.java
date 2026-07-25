package io.github.leawind.gitparcel.common.api.parcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSink;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.regex.Pattern;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A codec for portable parcel content.
 *
 * <p>Formats never access a Minecraft level directly. World capture, semantic processing, and
 * placement are owned by the parcel runtime.
 */
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
      if (version < 0) {
        throw new IllegalArgumentException("version must be non-negative");
      }
    }

    @NonNull
    @Override
    public String toString() {
      return "%s:%d".formatted(id, version);
    }

    @Override
    public int compareTo(ParcelFormat.@NonNull Spec other) {
      int byId = id.compareTo(other.id);
      return byId != 0 ? byId : Integer.compare(version, other.version);
    }
  }

  enum Capability {
    BLOCKS,
    BLOCK_ENTITIES,
    ENTITIES,
    ATTACHMENTS,
    OPAQUE_ATTACHMENTS
  }

  Spec spec();

  default EnumSet<Capability> capabilities() {
    return EnumSet.noneOf(Capability.class);
  }

  non-sealed interface Impl<C extends ParcelFormatConfig<C>> extends ParcelFormat {
    default @NonNull <T> C castConfig(@NonNull T config) throws ClassCastException {
      var clazz = configClass();
      if (clazz == null) {
        throw new ClassCastException(
            "Expected null, got %s: %s".formatted(config.getClass().getSimpleName(), config));
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

  interface Writer<C extends ParcelFormatConfig<C>> extends Impl<C> {
    /** Preferred maximum edge length for block sections requested from the content source. */
    int blockSectionSize();

    void write(WriteContext<C> context, ParcelContentSource source)
        throws IOException, ParcelException;
  }

  interface Reader<C extends ParcelFormatConfig<C>> extends Impl<C> {
    void read(ReadContext<C> context, ParcelContentSink sink)
        throws IOException, ParcelException;
  }

  abstract class BaseContext<C extends ParcelFormatConfig<C>> {
    private final Vec3i parcelSize;
    private final Vec3i anchor;
    private final int dataVersion;
    private final Path dataDir;
    private final @Nullable C config;

    protected BaseContext(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path dataDir,
        @Nullable C config) {
      this.parcelSize = parcelSize;
      this.anchor = anchor;
      this.dataVersion = dataVersion;
      this.dataDir = dataDir;
      this.config = config;
    }

    public Vec3i parcelSize() {
      return parcelSize;
    }

    public Vec3i anchor() {
      return anchor;
    }

    public int dataVersion() {
      return dataVersion;
    }

    public Path dataDir() {
      return dataDir;
    }

    public @Nullable C config() {
      return config;
    }
  }

  final class WriteContext<C extends ParcelFormatConfig<C>> extends BaseContext<C> {
    public WriteContext(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path dataDir,
        @Nullable C config) {
      super(parcelSize, anchor, dataVersion, dataDir, config);
    }
  }

  final class ReadContext<C extends ParcelFormatConfig<C>> extends BaseContext<C> {
    public ReadContext(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path dataDir,
        @Nullable C config) {
      super(parcelSize, anchor, dataVersion, dataDir, config);
    }
  }
}
