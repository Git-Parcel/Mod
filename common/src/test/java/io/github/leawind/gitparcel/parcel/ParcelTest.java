package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.leawind.gitparcel.utils.json.JsonAccessException;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelTest {
  static class ParcelMetaTest {
    @Test
    void testFromJson_MissingRequiredFields() {
      JsonObject json = new JsonObject();

      // Missing format
      assertThrows(
          JsonAccessException.MissingProperty.class, () -> Parcel.ParcelMeta.fromJson(json));

      // Add format but missing id
      JsonObject formatJson = new JsonObject();
      formatJson.addProperty("version", 1);
      json.add("format", formatJson);

      assertThrows(
          JsonAccessException.MissingProperty.class, () -> Parcel.ParcelMeta.fromJson(json));

      // Add id but missing dataVersion
      formatJson.addProperty("id", "test");

      assertThrows(
          JsonAccessException.MissingProperty.class, () -> Parcel.ParcelMeta.fromJson(json));

      // Add dataVersion but missing size
      json.addProperty("dataVersion", 1);

      assertThrows(
          JsonAccessException.MissingProperty.class, () -> Parcel.ParcelMeta.fromJson(json));
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
      Parcel.ParcelMeta meta = Parcel.ParcelMeta.fromJson(json);

      assertEquals("parcel", meta.formatId);
      assertEquals(1, meta.formatVersion);
      assertEquals(1536, meta.dataVersion);
      assertEquals(new Vec3i(15, 12, 20), meta.size);
      assertEquals("Steve's House", meta.name);
      assertEquals("This is steve's house", meta.description);
      assertEquals(java.util.List.of("building", "house"), meta.tags);
      assertEquals(Boolean.TRUE, meta.includeEntity);

      assertNotNull(meta.mods);
      assertEquals(4, meta.mods.size());
      assertNull(meta.mods.get("mod_1").min());
      assertNull(meta.mods.get("mod_1").max());
      assertEquals("1.0.0", meta.mods.get("mod_2").min());
      assertEquals("2.0.0", meta.mods.get("mod_2").max());
      assertEquals("1.0.0", meta.mods.get("mod_3").min());
      assertNull(meta.mods.get("mod_3").max());
      assertNull(meta.mods.get("mod_4").min());
      assertEquals("1.0.0", meta.mods.get("mod_4").max());

      assertTrue(meta.extra instanceof JsonObject);
      // assert custom_fields is obj
      assertTrue(meta.extra.has("custom_fields"));
      JsonObject customFields = meta.extra.getAsJsonObject("custom_fields");
      assertNotNull(customFields);
      assertEquals(
          "This schema allows custom fields like this", customFields.get("Wow").getAsString());

      // Test serialization
      JsonObject serialized = (JsonObject) meta.toJson();
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
      Parcel.ParcelMeta meta = Parcel.ParcelMeta.fromJson(json);

      assertEquals("test-format", meta.formatId);
      assertEquals(1, meta.formatVersion);
      assertEquals(12345, meta.dataVersion);
      assertEquals(new Vec3i(10, 20, 30), meta.size);
      assertNull(meta.name);
      assertNull(meta.description);
      assertNull(meta.tags);
      assertNull(meta.mods);
      assertNull(meta.includeEntity);

      // Test serialization
      JsonObject serialized = (JsonObject) meta.toJson();
      assertNotNull(serialized);
      assertNotNull(serialized.get("format"));
      assertNotNull(serialized.get("dataVersion"));
      assertNotNull(serialized.get("size"));
    }
  }
}
