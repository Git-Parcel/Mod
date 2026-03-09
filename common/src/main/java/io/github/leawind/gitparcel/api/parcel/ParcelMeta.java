package io.github.leawind.gitparcel.api.parcel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.GitParcelApi;
import io.github.leawind.gitparcel.api.parcel.exceptions.InvalidParcelMetaException;
import io.github.leawind.gitparcel.utils.json.JsonAccessException;
import io.github.leawind.gitparcel.utils.json.JsonObjectAccessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
                      ParcelFormat.Info.CODEC.fieldOf("format").forGetter(ParcelMeta::format),
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

  public static ParcelMeta create(String formatId, int formatVersion, Vec3i parcelSize) {
    return new ParcelMeta(
        formatId,
        formatVersion,
        SharedConstants.getCurrentVersion().dataVersion().version(),
        parcelSize);
  }

  public ParcelFormat.Info format;
  public int dataVersion;
  public Vec3i size;

  public @Nullable String name = null;
  public @Nullable String description = null;
  public @Nullable List<String> tags = null;
  public @Nullable Map<String, ModDependency> mods = null;

  /** Default is {@code true}. */
  public @Nullable Boolean excludeEntities = null;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private ParcelMeta(
      ParcelFormat.Info format,
      Integer dataVersion,
      Vec3i size,
      Optional<String> name,
      Optional<String> description,
      Optional<List<String>> tgs,
      Optional<Map<String, ModDependency>> mods,
      Optional<Boolean> excludeEntities) {
    this.format = format;
    this.dataVersion = dataVersion;
    this.size = size;
    this.name = name.orElse(null);
    this.description = description.orElse(null);
    this.tags = tgs.orElse(null);
    this.mods = mods.orElse(null);
    this.excludeEntities = excludeEntities.orElse(true);
  }

  private ParcelFormat.Info format() {
    return format;
  }

  private int dataVersion() {
    return dataVersion;
  }

  private Vec3i size() {
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
    return GitParcelApi.FORMAT_REGISTRY.getSaver(format.id(), format.version());
  }

  public ParcelFormat.@Nullable Load<?> getFormatLoader() {
    return GitParcelApi.FORMAT_REGISTRY.getLoader(format.id(), format.version());
  }

  public boolean excludeEntities() {
    return excludeEntities == null || excludeEntities;
  }

  private ParcelMeta(ParcelFormat.Info format, int dataVersion, Vec3i parcelSize) {
    this.format = format;
    this.dataVersion = dataVersion;
    this.size = parcelSize;
  }

  @Deprecated
  private ParcelMeta(String formatId, int formatVersion, int dataVersion, Vec3i parcelSize) {
    this(new ParcelFormat.Info(formatId, formatVersion), dataVersion, parcelSize);
  }

  /**
   * Save the metadata to the given file path.
   *
   * @param metaFile Path to the file. The parent directories will be created if not exist. File
   *     will be overwritten if it already exists.
   * @throws IOException If an I/O error occurs while writing the file
   */
  public void save(Path metaFile) throws IOException {
    Files.createDirectories(metaFile.getParent());
    Files.writeString(metaFile, GSON.toJson(toJsonObject()));
  }

  @Deprecated
  public JsonObject toJsonObject() {
    JsonObject json = new JsonObject();
    {
      JsonObject formatJson = new JsonObject();
      formatJson.addProperty("id", format.id());
      formatJson.addProperty("version", format.version());
      json.add("format", formatJson);
    }
    json.addProperty("dataVersion", dataVersion);
    {
      JsonArray sizeJson = new JsonArray();
      sizeJson.add(size.getX());
      sizeJson.add(size.getY());
      sizeJson.add(size.getZ());
      json.add("size", sizeJson);
    }
    if (name != null) {
      json.addProperty("name", name);
    }
    if (description != null) {
      json.addProperty("description", description);
    }
    if (tags != null) {
      JsonArray tagsJson = new JsonArray();
      tags.forEach(tagsJson::add);
      json.add("tags", tagsJson);
    }
    if (mods != null) {
      JsonObject modsJson = new JsonObject();
      for (var entry : mods.entrySet()) {
        var dep = entry.getValue();
        JsonObject modJson = new JsonObject();
        modJson.addProperty("min", dep.min());
        modJson.addProperty("max", dep.max());
        modsJson.add(entry.getKey(), modJson);
      }
      json.add("mods", modsJson);
    }
    json.addProperty("excludeEntities", excludeEntities);

    return json;
  }

  /**
   * @param metaFile File path to the file
   * @return The parsed {@link ParcelMeta} object
   * @throws IOException If an I/O error occurs while reading the file
   */
  public static ParcelMeta load(Path metaFile) throws IOException, InvalidParcelMetaException {
    try {
      var json = GSON.fromJson(Files.readString(metaFile), JsonObject.class);
      return fromJsonObject(json);
    } catch (JsonAccessException e) {
      throw new InvalidParcelMetaException("Invalid parcel metadata at " + metaFile, e);
    }
  }

  @Deprecated
  public static ParcelMeta fromJsonObject(JsonObject json) throws JsonAccessException {
    var ja = new JsonObjectAccessor(json);
    ParcelMeta meta;

    {
      String formatId;
      int formatVersion;
      {
        ja.requireJsonObject("format");
        formatId = ja.requireString("format", "id");
        formatVersion = ja.requireNumber("format", "version").intValue();
        json.remove("format");
      }
      int dataVersion = ja.requireNumber("dataVersion").intValue();
      json.remove("dataVersion");

      Vec3i size;
      {
        JsonArray sizeJson = ja.requireJsonArray("size");
        json.remove("size");
        int sizeX = sizeJson.get(0).getAsInt();
        int sizeY = sizeJson.get(1).getAsInt();
        int sizeZ = sizeJson.get(2).getAsInt();
        size = new Vec3i(sizeX, sizeY, sizeZ);
      }
      meta = new ParcelMeta(formatId, formatVersion, dataVersion, size);
    }

    {
      meta.name = ja.optionalString("name");
      json.remove("name");
    }

    {
      meta.description = ja.optionalString("description");
      json.remove("description");
    }
    if (json.has("tags")) {
      var tagsJson = ja.requireJsonArray("tags");
      List<String> tags = new ArrayList<>(tagsJson.size());
      for (JsonElement tagElement : tagsJson) {
        tags.add(tagElement.getAsString());
      }
      meta.tags = tags;
      json.remove("tags");
    }

    if (json.has("mods")) {
      var modsJson = ja.requireJsonObject("mods");
      meta.mods = new HashMap<>();

      for (var entry : modsJson.entrySet()) {
        // "*" | {min?, max?}
        JsonElement depJson = entry.getValue();
        if (depJson.isJsonObject()) {
          var modJson = depJson.getAsJsonObject();
          var modJa = new JsonObjectAccessor(modJson);
          meta.mods.put(
              entry.getKey(),
              new ModDependency(
                  modJa.optionalString("min"), //
                  modJa.optionalString("max"),
                  null));
        } else {
          var s = JsonObjectAccessor.requireString(depJson);
          if (!s.equals("*")) {
            throw new JsonAccessException.IncorrectType("\"*\"", "\"" + s + "\"");
          }
          meta.mods.put(entry.getKey(), ModDependency.ANY);
        }
      }
      json.remove("mods");
    }

    {
      meta.excludeEntities = ja.optionalBool("excludeEntities");
      json.remove("excludeEntities");
    }
    // meta.extra = json;
    return meta;
  }
}
