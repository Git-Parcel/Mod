package io.github.leawind.gitparcel.common.api.parcel.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.operation.ProgressReporter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * One independently versioned kind of parcel content stored below a directory named by its id.
 *
 * <p>Implementations own the complete contents of their directory. Saving must reconcile inherited
 * files into one complete current result; loading must reject unexpected or malformed files.
 */
public interface ParcelContentType<C extends ParcelContentConfig<C>> {
  record Spec(String id, int version) implements Comparable<Spec> {
    public static final Pattern ID_PATTERN = Pattern.compile("[a-z][a-z0-9_.-]{0,63}");
    public static final Codec<Spec> CODEC =
        RecordCodecBuilder.create(
            instance ->
                instance
                    .group(
                        Codec.STRING.fieldOf("id").forGetter(Spec::id),
                        Codec.INT.fieldOf("version").forGetter(Spec::version))
                    .apply(instance, Spec::new));

    public Spec {
      Objects.requireNonNull(id, "id");
      if (!ID_PATTERN.matcher(id).matches() || id.equals(".") || id.equals("..")) {
        throw new IllegalArgumentException(
            "Parcel content id must be one safe directory name matching " + ID_PATTERN + ": " + id);
      }
      if (version < 0) {
        throw new IllegalArgumentException("Parcel content version must be non-negative");
      }
    }

    @Override
    public int compareTo(Spec other) {
      int byId = id.compareTo(other.id);
      return byId != 0 ? byId : Integer.compare(version, other.version);
    }

    @Override
    public String toString() {
      return id + ":" + version;
    }
  }

  Spec spec();

  /** Content ids that must be loaded before this content type. Save order is the reverse. */
  default Set<String> loadAfter() {
    return Set.of();
  }

  default @Nullable C defaultConfig() {
    return null;
  }

  default @Nullable Class<C> configClass() {
    return null;
  }

  default <T> C castConfig(T config) {
    Class<C> type = configClass();
    if (type == null) {
      throw new ClassCastException("Content type " + spec() + " does not accept configuration");
    }
    return type.cast(config);
  }

  void save(SaveContext<C> context, ParcelDataSource source)
      throws IOException, ParcelException;

  void load(LoadContext<C> context, ParcelDataSink sink)
      throws IOException, ParcelException;

  abstract class Context<C extends ParcelContentConfig<C>> {
    private final Vec3i parcelSize;
    private final Vec3i anchor;
    private final int dataVersion;
    private final Path directory;
    private final @Nullable C config;
    private final ProgressReporter progress;

    protected Context(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path directory,
        @Nullable C config,
        ProgressReporter progress) {
      this.parcelSize = Objects.requireNonNull(parcelSize, "parcelSize");
      this.anchor = Objects.requireNonNull(anchor, "anchor");
      this.dataVersion = dataVersion;
      this.directory = Objects.requireNonNull(directory, "directory");
      this.config = config;
      this.progress = ProgressReporter.safe(progress);
    }

    public final Vec3i parcelSize() {
      return parcelSize;
    }

    public final Vec3i anchor() {
      return anchor;
    }

    public final int dataVersion() {
      return dataVersion;
    }

    /** Root directory exclusively owned by this content type. */
    public final Path directory() {
      return directory;
    }

    public final @Nullable C config() {
      return config;
    }

    public final ProgressReporter progress() {
      return progress;
    }
  }

  final class SaveContext<C extends ParcelContentConfig<C>> extends Context<C> {
    public SaveContext(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path directory,
        @Nullable C config,
        ProgressReporter progress) {
      super(parcelSize, anchor, dataVersion, directory, config, progress);
    }
  }

  final class LoadContext<C extends ParcelContentConfig<C>> extends Context<C> {
    public LoadContext(
        Vec3i parcelSize,
        Vec3i anchor,
        int dataVersion,
        Path directory,
        @Nullable C config,
        ProgressReporter progress) {
      super(parcelSize, anchor, dataVersion, directory, config, progress);
    }
  }
}
