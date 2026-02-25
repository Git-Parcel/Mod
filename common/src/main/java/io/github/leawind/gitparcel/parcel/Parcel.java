package io.github.leawind.gitparcel.parcel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import org.jspecify.annotations.Nullable;

public final class Parcel {

  /**
   * Metadata for parcels.
   *
   * @see <a href="https://git-parcel.github.io/schemas/ParcelMeta.json">Parcel Metadata Schema</a>
   */
  public static final class Metadata {
    public static final String FILE_NAME = "parcel.json";
    public static final String SCHEMA_URL = "https://git-parcel.github.io/schemas/ParcelMeta.json";

    public record ModDependency(@Nullable String min, @Nullable String max) {}

    public ParcelFormat format;
    public int dataVersion;
    public Vec3i size;

    public @Nullable String name = null;
    public @Nullable String description = null;
    public @Nullable List<String> tags = null;
    public @Nullable Map<String, ModDependency> mods = null;
    public @Nullable Boolean includeEntity = null;

    /** Extra fields. */
    public JsonObject extra = new JsonObject();

    private Metadata(ParcelFormat format, int dataVersion, Vec3i size) {
      this.format = format;
      this.dataVersion = dataVersion;
      this.size = size;
      extra.addProperty("$schema", SCHEMA_URL);
    }

    public JsonElement toJson() {
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
      json.addProperty("includeEntity", includeEntity);

      // extra fields
      for (var entry : extra.entrySet()) {
        json.add(entry.getKey(), entry.getValue());
      }

      return json;
    }

    public static Metadata create(ParcelFormat format, Vec3i size) {
      return new Metadata(
          format, SharedConstants.getCurrentVersion().dataVersion().version(), size);
    }

    public static Metadata fromJson(JsonObject json) {
      //TODO
      return null;
    }
  }
}
