package io.github.leawind.gitparcel.api.parcel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.exceptions.InvalidParcelMetaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * Metadata for parcels.
 *
 * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
 */
public final class ParcelMeta {
  @Deprecated private static final Gson GSON = new Gson();
  public static final String SCHEMA_URL = "https://git-parcel.github.io/schemas/ParcelMeta.json";

  public static final Codec<ParcelMeta> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Info.CODEC.fieldOf("format").forGetter(ParcelMeta::formatInfo),
                      Codec.INT.fieldOf("dataVersion").forGetter(ParcelMeta::dataVersion),
                      Vec3i.CODEC.fieldOf("size").forGetter(ParcelMeta::size),
                      Codec.STRING.optionalFieldOf("name").forGetter(ParcelMeta::getName),
                      Codec.STRING
                          .optionalFieldOf("description")
                          .forGetter(ParcelMeta::getDescription),
                      Codec.STRING.listOf().optionalFieldOf("tags").forGetter(ParcelMeta::getTags),
                      Codec.unboundedMap(Codec.STRING, ModDependency.CODEC)
                          .optionalFieldOf("mods")
                          .forGetter(ParcelMeta::getMods),
                      Codec.BOOL
                          .optionalFieldOf("excludeEntities")
                          .forGetter(ParcelMeta::getExcludeEntities))
                  .apply(inst, ParcelMeta::new));

  private ParcelFormat.Info formatInfo;
  private int dataVersion;
  private Vec3i size;

  private @Nullable String name = null;
  private @Nullable String description = null;
  private @Nullable List<String> tags = null;
  private @Nullable Map<String, ModDependency> mods = null;

  /** Default is {@code true}. */
  public @Nullable Boolean excludeEntities = null;

  @Deprecated
  public static ParcelMeta create(String formatId, int formatVersion, Vec3i parcelSize) {
    return new ParcelMeta(
        formatId,
        formatVersion,
        SharedConstants.getCurrentVersion().dataVersion().version(),
        parcelSize);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private ParcelMeta(
      ParcelFormat.Info formatInfo,
      Integer dataVersion,
      Vec3i size,
      Optional<String> name,
      Optional<String> description,
      Optional<List<String>> tgs,
      Optional<Map<String, ModDependency>> mods,
      Optional<Boolean> excludeEntities) {
    this.formatInfo = formatInfo;
    this.dataVersion = dataVersion;
    this.size = size;
    this.name = name.orElse(null);
    this.description = description.orElse(null);
    this.tags = tgs.orElse(null);
    this.mods = mods.orElse(null);
    this.excludeEntities = excludeEntities.orElse(true);
  }

  public ParcelMeta(ParcelFormat.Info formatInfo, Vec3i parcelSize) {
    this(formatInfo, SharedConstants.getCurrentVersion().dataVersion().version(), parcelSize);
  }

  public ParcelMeta(ParcelFormat.Info formatInfo, int dataVersion, Vec3i parcelSize) {
    this.formatInfo = formatInfo;
    this.dataVersion = dataVersion;
    this.size = parcelSize;
  }

  @Deprecated
  private ParcelMeta(String formatId, int formatVersion, int dataVersion, Vec3i parcelSize) {
    this(new ParcelFormat.Info(formatId, formatVersion), dataVersion, parcelSize);
  }

  public ParcelFormat.Info formatInfo() {
    return formatInfo;
  }

  public int dataVersion() {
    return dataVersion;
  }

  public Vec3i size() {
    return size;
  }

  private Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  private Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public Optional<List<String>> getTags() {
    return Optional.ofNullable(tags);
  }

  public Optional<Map<String, ModDependency>> getMods() {
    return Optional.ofNullable(mods);
  }

  public Optional<Boolean> getExcludeEntities() {
    return Optional.ofNullable(excludeEntities);
  }

  public ParcelFormat.@Nullable Save<?> getFormatSaver() {
    return ParcelFormatRegistry.INSTANCE.getSaver(formatInfo);
  }

  public ParcelFormat.@Nullable Load<?> getFormatLoader() {
    return ParcelFormatRegistry.INSTANCE.getLoader(formatInfo);
  }

  public boolean excludeEntities() {
    return excludeEntities == null || excludeEntities;
  }

  /**
   * Save the metadata to the given file path.
   *
   * @param file Path to the file. The parent directories will be created if not exist. File will be
   *     overwritten if it already exists.
   * @throws IOException If an I/O error occurs while writing the file
   * @throws IllegalStateException If the metadata is not valid
   */
  public void save(Path file) throws IOException, IllegalStateException {
    Files.createDirectories(file.getParent());
    var result = CODEC.encodeStart(JsonOps.INSTANCE, this);
    Files.writeString(file, GSON.toJson((JsonObject) result.getOrThrow()));
  }

  public record ModDependency(
      @Nullable String min, @Nullable String max, @Nullable List<String> namespaces) {
    public static final Codec<ModDependency> CODEC =
        RecordCodecBuilder.create(
            inst ->
                inst.group(
                        Codec.STRING.optionalFieldOf("min").forGetter(ModDependency::getMin),
                        Codec.STRING.optionalFieldOf("max").forGetter(ModDependency::getMax),
                        Codec.STRING
                            .listOf()
                            .optionalFieldOf("namespaces")
                            .forGetter(ModDependency::getNamespaces))
                    .apply(inst, ModDependency::new));

    public static final ModDependency ANY = new ModDependency((String) null, null, null);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ModDependency(
        Optional<String> min, Optional<String> max, Optional<List<String>> namespaces) {
      this(min.orElse(null), max.orElse(null), namespaces.orElse(null));
    }

    public Optional<String> getMin() {
      return Optional.ofNullable(min);
    }

    public Optional<String> getMax() {
      return Optional.ofNullable(max);
    }

    public Optional<List<String>> getNamespaces() {
      return Optional.ofNullable(namespaces);
    }
  }

  /**
   * @param metaFile File path to the file
   * @return The parsed {@link ParcelMeta} object
   * @throws IOException If an I/O error occurs while reading the file
   * @throws InvalidParcelMetaException If the file content is not valid
   */
  public static ParcelMeta load(Path metaFile) throws IOException, InvalidParcelMetaException {
    try {
      var json = GSON.fromJson(Files.readString(metaFile), JsonObject.class);
      var result = CODEC.parse(JsonOps.INSTANCE, json);
      return result.getOrThrow();
    } catch (IllegalStateException e) {
      throw new InvalidParcelMetaException("Invalid parcel metadata at " + metaFile, e);
    }
  }
}
