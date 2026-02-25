package io.github.leawind.gitparcel.parcel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

public final class Parcel {

  /** Custom exception for parcel-related errors. */
  public static class ParcelException extends RuntimeException {
    public ParcelException(String message) {
      super(message);
    }

    public ParcelException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Metadata for parcels.
   *
   * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
   */
  public static final class Metadata {
    /** Exceptions thrown when parsing or validating parcel metadata. */
    public static class ParcelMetadataException extends ParcelException {
      public ParcelMetadataException(String message) {
        super(message);
      }

      public ParcelMetadataException(String message, Throwable cause) {
        super(message, cause);
      }
    }

    public static final String FILE_NAME = "parcel.json";
    public static final String SCHEMA_URL = "https://git-parcel.github.io/schemas/ParcelMeta.json";

    public record ModDependency(@Nullable String min, @Nullable String max) {}

    public String formatId;
    public int formatVersion;
    public int dataVersion;
    public Vec3i size;

    public @Nullable String name = null;
    public @Nullable String description = null;
    public @Nullable List<String> tags = null;
    public @Nullable Map<String, ModDependency> mods = null;
    public @Nullable Boolean includeEntity = null;

    /** Extra fields. */
    public JsonObject extra = new JsonObject();

    private Metadata(String formatId, int formatVersion, int dataVersion, Vec3i size) {
      this.formatId = formatId;
      this.formatVersion = formatVersion;
      this.dataVersion = dataVersion;
      this.size = size;
      extra.addProperty("$schema", SCHEMA_URL);
    }

    public JsonElement toJson() {
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

    public static Metadata create(String formatId, int formatVersion, Vec3i size) {
      return new Metadata(
          formatId,
          formatVersion,
          SharedConstants.getCurrentVersion().dataVersion().version(),
          size);
    }

    public static Metadata fromJson(JsonObject json) throws ParcelMetadataException {
      Metadata metadata;
      // Parse required fields
      // TODO throw if missing: `Missing property "format"`
      // TODO throw if type incorrect: `Incorrect type. Expected "number", got "string"`
      {
        String formatId;
        int formatVersion;
        {
          JsonObject formatJson = json.getAsJsonObject("format");
          json.remove("format");
          formatId = formatJson.get("id").getAsString();
          formatVersion = formatJson.get("version").getAsInt();
        }
        int dataVersion = json.get("dataVersion").getAsInt();

        Vec3i size;
        {
          JsonArray sizeJson = json.getAsJsonArray("size");
          json.remove("size");
          int sizeX = sizeJson.get(0).getAsInt();
          int sizeY = sizeJson.get(1).getAsInt();
          int sizeZ = sizeJson.get(2).getAsInt();
          size = new Vec3i(sizeX, sizeY, sizeZ);
        }
        metadata = new Metadata(formatId, formatVersion, dataVersion, size);
      }

      // Parse optional fields
      // TODO throw if type incorrect: `Incorrect type. Expected "number", got "string"`
      if (json.has("name")) {
        metadata.name = json.get("name").getAsString();
        json.remove("name");
      }
      if (json.has("description")) {
        metadata.description = json.get("description").getAsString();
        json.remove("description");
      }
      if (json.has("tags")) {
        JsonArray tagsJson = json.getAsJsonArray("tags");
        json.remove("tags");
        List<String> tags = new ArrayList<>(tagsJson.size());
        for (JsonElement tagElement : tagsJson) {
          tags.add(tagElement.getAsString());
        }
        metadata.tags = tags;
      }
      if (json.has("mods")) {
        JsonObject modsJson = json.getAsJsonObject("mods");
        json.remove("mods");
        Map<String, ModDependency> mods = new HashMap<>();
        for (var entry : modsJson.entrySet()) {
          String modId = entry.getKey();
          JsonObject modJson = entry.getValue().getAsJsonObject();
          String min = modJson.has("min") ? modJson.get("min").getAsString() : null;
          String max = modJson.has("max") ? modJson.get("max").getAsString() : null;
          mods.put(modId, new ModDependency(min, max));
        }
        metadata.mods = mods;
      }
      if (json.has("includeEntity")) {
        metadata.includeEntity = json.get("includeEntity").getAsBoolean();
        json.remove("includeEntity");
      }
      metadata.extra = json;
      return metadata;
    }
  }
}
