package io.github.leawind.gitparcel.parcel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.utils.json.JsonAccessException;
import io.github.leawind.gitparcel.utils.json.JsonObjectAccessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

/**
 * Metadata for parcels.
 *
 * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
 */
public final class ParcelMeta {
  public static final String FILE_NAME = "parcel.json";

  public record ModDependency(@Nullable String min, @Nullable String max) {
    public static final ModDependency ANY = new ModDependency(null, null);
  }

  /** Exceptions thrown when parsing or validating parcel metadata. */
  public static class InvalidParcelMetaException extends Parcel.ParcelException.InvalidParcel {
    public InvalidParcelMetaException(String message) {
      super(message);
    }

    public InvalidParcelMetaException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static final Gson GSON = new Gson();
  public static final String SCHEMA_URL = "https://git-parcel.github.io/schemas/ParcelMeta.json";

  public static ParcelMeta create(String formatId, int formatVersion, Vec3i size) {
    return new ParcelMeta(
        formatId, formatVersion, SharedConstants.getCurrentVersion().dataVersion().version(), size);
  }

  public String formatId;
  public int formatVersion;
  public int dataVersion;

  public Vec3i size;
  public @Nullable String name = null;
  public @Nullable String description = null;
  public @Nullable List<String> tags = null;
  public @Nullable Map<String, ModDependency> mods = null;

  /** Default is {@code true}. */
  public @Nullable Boolean includeEntity = null;

  /** Extra fields. */
  public JsonObject extra = new JsonObject();

  public boolean includeEntity() {
    return includeEntity == null || includeEntity;
  }

  private ParcelMeta(String formatId, int formatVersion, int dataVersion, Vec3i size) {
    this.formatId = formatId;
    this.formatVersion = formatVersion;
    this.dataVersion = dataVersion;
    this.size = size;
    extra.addProperty("$schema", SCHEMA_URL);
  }

  /**
   * Save the metadata to the given directory.
   *
   * <ul>
   *   <li>Overwrite the file if it already exists.
   *   <li>Create the parent directories if they do not exist.
   * </ul>
   *
   * @param dirPath The parcel directory to save the metadata file to
   * @throws IOException If an I/O error occurs while writing the file
   */
  public void saveToParcelDir(Path dirPath) throws IOException {
    save(dirPath.resolve(FILE_NAME));
  }

  /**
   * Save the metadata to the given file path.
   *
   * <ul>
   *   <li>Overwrite the file if it already exists.
   *   <li>Create the parent directories if they do not exist.
   * </ul>
   *
   * @param filePath Path to the metadata file
   * @throws IOException If an I/O error occurs while writing the file
   */
  public void save(Path filePath) throws IOException {
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, GSON.toJson(toJsonObject()));
  }

  public JsonObject toJsonObject() {
    JsonObject json = new JsonObject();
    {
      JsonObject formatJson = new JsonObject();
      formatJson.addProperty("id", formatId);
      formatJson.addProperty("version", formatVersion);
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
    json.addProperty("includeEntity", includeEntity);

    // extra fields
    for (var entry : extra.entrySet()) {
      json.add(entry.getKey(), entry.getValue());
    }

    return json;
  }

  /**
   * @param path File path to the parcel metadata file
   * @return The parsed {@link ParcelMeta} object
   * @throws IOException If an I/O error occurs while reading the file
   */
  public static ParcelMeta load(Path path) throws IOException, InvalidParcelMetaException {
    var json = GSON.fromJson(Files.readString(path), JsonObject.class);
    try {
      return fromJsonObject(json);
    } catch (JsonAccessException e) {
      throw new InvalidParcelMetaException("Invalid parcel metadata at " + path, e);
    }
  }

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
                  modJa.optionalString("max")));
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
      meta.includeEntity = ja.optionalBool("includeEntity");
      json.remove("includeEntity");
    }
    meta.extra = json;
    return meta;
  }
}
