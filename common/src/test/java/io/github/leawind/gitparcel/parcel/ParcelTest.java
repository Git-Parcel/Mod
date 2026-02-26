package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.utils.json.JsonAccessException;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelTest {
  static class MetadataTest {
    @Test
    void testFromJson_MissingRequiredFields() {
      JsonObject json = new JsonObject();

      // Missing format
      assertThrows(JsonAccessException.MissingProperty.class, () -> Parcel.Metadata.fromJson(json));

      // Add format but missing id
      JsonObject formatJson = new JsonObject();
      formatJson.addProperty("version", 1);
      json.add("format", formatJson);

      assertThrows(JsonAccessException.MissingProperty.class, () -> Parcel.Metadata.fromJson(json));

      // Add id but missing dataVersion
      formatJson.addProperty("id", "test");

      assertThrows(JsonAccessException.MissingProperty.class, () -> Parcel.Metadata.fromJson(json));

      // Add dataVersion but missing size
      json.addProperty("dataVersion", 1);

      assertThrows(JsonAccessException.MissingProperty.class, () -> Parcel.Metadata.fromJson(json));
    }

    @Test
    void testToJsonAndFromJson() throws JsonAccessException {
      String jsonStr =
"""
{
  "format": {
    "id": "parcel",
    "version": 1
  },
  "dataVersion": 1536,
  "size": [15, 12, 20],
  "mods": {
    "mod_1": "*",
    "mod_2": { "min": "1.0.0", "max": "2.0.0" },
    "mod_3": { "min": "1.0.0" },
    "mod_4": { "max": "1.0.0" }
  },
  "includeEntity": true,
  "name": "Steve's House",
  "description": "This is steve's house",
  "tags": [
    "building",
    "house"
  ],
  "custom_fields": {
    "Wow": "This schema allows custom fields like this"
  }
}
""";
      JsonObject json = new Gson().fromJson(jsonStr, JsonObject.class);

      // Test deserialization
      Parcel.Metadata metadata = Parcel.Metadata.fromJson(json);

      assertEquals("parcel", metadata.formatId);
      assertEquals(1, metadata.formatVersion);
      assertEquals(1536, metadata.dataVersion);
      assertEquals(new Vec3i(15, 12, 20), metadata.size);
      assertEquals("Steve's House", metadata.name);
      assertEquals("This is steve's house", metadata.description);
      assertEquals(java.util.List.of("building", "house"), metadata.tags);
      assertEquals(Boolean.TRUE, metadata.includeEntity);

      assertNotNull(metadata.mods);
      assertEquals(4, metadata.mods.size());
      assertNull(metadata.mods.get("mod_1").min());
      assertNull(metadata.mods.get("mod_1").max());
      assertEquals("1.0.0", metadata.mods.get("mod_2").min());
      assertEquals("2.0.0", metadata.mods.get("mod_2").max());
      assertEquals("1.0.0", metadata.mods.get("mod_3").min());
      assertNull(metadata.mods.get("mod_3").max());
      assertNull(metadata.mods.get("mod_4").min());
      assertEquals("1.0.0", metadata.mods.get("mod_4").max());

      assertTrue(metadata.extra instanceof JsonObject);
      // assert custom_fields is obj
      assertTrue(metadata.extra.has("custom_fields"));
      JsonObject customFields = metadata.extra.getAsJsonObject("custom_fields");
      assertNotNull(customFields);
      assertEquals(
          "This schema allows custom fields like this", customFields.get("Wow").getAsString());

      // Test serialization
      JsonObject serialized = (JsonObject) metadata.toJson();
      assertNotNull(serialized);
      assertFalse(serialized.has("$schema"));
      assertTrue(serialized.has("format"));
      assertTrue(serialized.has("dataVersion"));
      assertTrue(serialized.has("size"));
      assertTrue(serialized.has("name"));
      assertTrue(serialized.has("description"));
      assertTrue(serialized.has("tags"));
      assertTrue(serialized.has("mods"));
      assertTrue(serialized.has("includeEntity"));
      assertTrue(serialized.has("custom_fields"));
    }

    @Test
    void testToJsonAndFromJson_Minimal() throws JsonAccessException {
      // Create a minimal JSON object manually
      JsonObject json = new JsonObject();

      // Only required fields
      JsonObject formatJson = new JsonObject();
      formatJson.addProperty("id", "test-format");
      formatJson.addProperty("version", 1);
      json.add("format", formatJson);

      json.addProperty("dataVersion", 12345);

      com.google.gson.JsonArray sizeJson = new com.google.gson.JsonArray();
      sizeJson.add(10);
      sizeJson.add(20);
      sizeJson.add(30);
      json.add("size", sizeJson);

      // Test deserialization
      Parcel.Metadata metadata = Parcel.Metadata.fromJson(json);

      assertEquals("test-format", metadata.formatId);
      assertEquals(1, metadata.formatVersion);
      assertEquals(12345, metadata.dataVersion);
      assertEquals(new Vec3i(10, 20, 30), metadata.size);
      assertNull(metadata.name);
      assertNull(metadata.description);
      assertNull(metadata.tags);
      assertNull(metadata.mods);
      assertNull(metadata.includeEntity);

      // Test serialization
      JsonObject serialized = (JsonObject) metadata.toJson();
      assertNotNull(serialized);
      assertNotNull(serialized.get("format"));
      assertNotNull(serialized.get("dataVersion"));
      assertNotNull(serialized.get("size"));
    }
  }
}
