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
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Metadata for parcels.
 *
 * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
 */
public final class ParcelMeta {
  private static final Gson GSON = new Gson();
  public static final String SCHEMA_URL = "https://git-parcel.github.io/schemas/ParcelMeta.json";

  public static final Codec<ParcelMeta> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Spec.CODEC.fieldOf("format").forGetter(ParcelMeta::formatSpec),
                      Codec.INT.fieldOf("dataVersion").forGetter(ParcelMeta::dataVersion),
                      Vec3i.CODEC.fieldOf("size").forGetter(ParcelMeta::size),
                      Vec3i.CODEC.fieldOf("anchor").forGetter(ParcelMeta::anchor),
                      Codec.STRING
                          .optionalFieldOf("key")
                          .forGetter(d -> Optional.ofNullable(d.name)),
                      Codec.STRING
                          .optionalFieldOf("description")
                          .forGetter(d -> Optional.ofNullable(d.description)),
                      Codec.STRING
                          .optionalFieldOf("author")
                          .forGetter(d -> Optional.ofNullable(d.author)),
                      Codec.STRING
                          .listOf()
                          .optionalFieldOf("tags")
                          .forGetter(d -> Optional.ofNullable(d.tags)),
                      Codec.unboundedMap(Codec.STRING, ModDependency.CODEC)
                          .optionalFieldOf("mods")
                          .forGetter(d -> Optional.ofNullable(d.mods)),
                      Codec.BOOL
                          .optionalFieldOf("exclude_entities")
                          .forGetter(d -> Optional.ofNullable(d.excludeEntities)))
                  .apply(inst, ParcelMeta::new));

  //  public static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P} ]{1,255}$");
  public static final Pattern NAME_PATTERN =
      Pattern.compile("^(?!.* {2,})[\\p{L}\\p{N}\\p{P} ]{1,255}$");

  public static boolean isValidDisplayName(String name) {
    return NAME_PATTERN.matcher(name).matches();
  }

  private ParcelFormat.Spec formatSpec;
  private int dataVersion;
  private Vec3i size;
  private Vec3i anchor;

  /**
   * @see #NAME_PATTERN
   */
  private @Nullable String name = null;

  private @Nullable String description = null;
  private @Nullable String author = null;
  private @Nullable List<String> tags = null;
  private @Nullable Map<String, ModDependency> mods = null;

  /** Default is {@code true}. */
  public @Nullable Boolean excludeEntities = null;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private ParcelMeta(
      ParcelFormat.Spec formatSpec,
      Integer dataVersion,
      Vec3i size,
      Vec3i anchor,
      Optional<String> name,
      Optional<String> description,
      Optional<String> author,
      Optional<List<String>> tgs,
      Optional<Map<String, ModDependency>> mods,
      Optional<Boolean> excludeEntities) {
    this.formatSpec = formatSpec;
    this.dataVersion = dataVersion;
    this.size = size;
    this.anchor = anchor;
    this.name = name.orElse(null);
    this.description = description.orElse(null);
    this.author = author.orElse(null);
    this.tags = tgs.orElse(null);
    this.mods = mods.orElse(null);
    this.excludeEntities = excludeEntities.orElse(true);
  }

  public ParcelMeta(ParcelFormat.Spec formatSpec, Vec3i parcelSize, Vec3i anchor) {
    this(
        formatSpec,
        SharedConstants.getCurrentVersion().dataVersion().version(),
        parcelSize,
        anchor);
  }

  public ParcelMeta(ParcelFormat.Spec formatSpec, int dataVersion, Vec3i parcelSize, Vec3i anchor) {
    this.formatSpec = formatSpec;
    this.dataVersion = dataVersion;
    this.size = parcelSize;
    this.anchor = anchor;
  }

  public ParcelFormat.Spec formatSpec() {
    return formatSpec;
  }

  public int dataVersion() {
    return dataVersion;
  }

  public Vec3i size() {
    return size;
  }

  public Vec3i anchor() {
    return anchor;
  }

  public @Nullable String name() {
    return name;
  }

  public @Nullable String description() {
    return description;
  }

  public boolean getExcludeEntities() {
    return Boolean.TRUE.equals(excludeEntities);
  }

  public ParcelFormat.@Nullable Saver<?> getFormatSaver() {
    return ParcelFormatRegistry.INSTANCE.getSaver(formatSpec);
  }

  public ParcelFormat.@Nullable Loader<?> getFormatLoader() {
    return ParcelFormatRegistry.INSTANCE.getLoader(formatSpec);
  }

  public void setFormatSpec(ParcelFormat.Spec formatSpec) {
    this.formatSpec = formatSpec;
  }

  /** Sets the name. */
  public void setName(@Nullable String name) throws IllegalArgumentException {
    if (!isValidDisplayName(name)) {
      throw new IllegalArgumentException("Invalid name: " + name);
    }
    this.name = name;
  }

  /** Sets the author. */
  public void setAuthor(@Nullable String author) {
    this.author = author;
  }

  /** Sets the description. */
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  /** Sets whether entities should be excluded. */
  public void setExcludeEntities(@Nullable Boolean excludeEntities) {
    this.excludeEntities = excludeEntities;
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
                        Codec.STRING
                            .optionalFieldOf("min")
                            .forGetter(d -> Optional.ofNullable(d.min)),
                        Codec.STRING
                            .optionalFieldOf("max")
                            .forGetter(d -> Optional.ofNullable(d.max)),
                        Codec.STRING
                            .listOf()
                            .optionalFieldOf("namespaces")
                            .forGetter(d -> Optional.ofNullable(d.namespaces)))
                    .apply(inst, ModDependency::new));

    public static final ModDependency ANY = new ModDependency((String) null, null, null);

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ModDependency(
        Optional<String> min, Optional<String> max, Optional<List<String>> namespaces) {
      this(min.orElse(null), max.orElse(null), namespaces.orElse(null));
    }

    public @Nullable String min() {
      return min;
    }

    public @Nullable String max() {
      return max;
    }

    public @Nullable List<String> namespaces() {
      return namespaces;
    }
  }

  public static ParcelMeta from(
      ParcelFormat.Spec format, BoundingBox boundingBox, Rotation rotation) {
    Vec3i sizeWorldSpace =
        new Vec3i(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
    Vec3i sizeParcelSpace = ParcelTransform.rotateSize(rotation, sizeWorldSpace);
    return new ParcelMeta(format, sizeParcelSpace, Vec3i.ZERO);
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
