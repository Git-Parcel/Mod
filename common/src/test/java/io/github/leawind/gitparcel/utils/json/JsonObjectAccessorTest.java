package io.github.leawind.gitparcel.utils.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

public class JsonObjectAccessorTest {
  private JsonObject createTestJsonObject() {
    JsonObject json = new JsonObject();
    json.addProperty("stringValue", "test");
    json.addProperty("numberValue", 42);
    json.addProperty("boolValue", true);

    JsonObject nested = new JsonObject();
    nested.addProperty("nestedString", "nestedTest");
    json.add("nested", nested);

    JsonArray array = new JsonArray();
    array.add("item1");
    array.add("item2");
    json.add("array", array);

    return json;
  }

  @Test
  void testRequireJsonElement_ValidSinglePath() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var element = accessor.requireJsonElement("stringValue");
    assertNotNull(element);
    assertEquals("test", element.getAsString());
  }

  @Test
  void testRequireJsonElement_ValidNestedPath() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var element = accessor.requireJsonElement("nested", "nestedString");
    assertNotNull(element);
    assertEquals("nestedTest", element.getAsString());
  }

  @Test
  void testRequireJsonElement_MissingProperty() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireJsonElement("missing"));
  }

  @Test
  void testRequireJsonElement_MissingNestedProperty() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class,
        () -> accessor.requireJsonElement("nested", "missing"));
  }

  @Test
  void testRequireJsonElement_IncorrectTypeInPath() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class,
        () -> accessor.requireJsonElement("stringValue", "someKey"));
  }

  @Test
  void testRequireJsonObject_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireJsonObject("nested");
    assertNotNull(result);
    assertEquals("nestedTest", result.get("nestedString").getAsString());
  }

  @Test
  void testRequireJsonObject_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireJsonObject("missing"));
  }

  @Test
  void testRequireJsonObject_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireJsonObject("stringValue"));
  }

  @Test
  void testRequireJsonArray_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireJsonArray("array");
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("item1", result.get(0).getAsString());
  }

  @Test
  void testRequireJsonArray_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireJsonArray("missing"));
  }

  @Test
  void testRequireJsonArray_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireJsonArray("stringValue"));
  }

  @Test
  void testRequireJsonPrimitive_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireJsonPrimitive("stringValue");
    assertNotNull(result);
    assertTrue(result.isString());
    assertEquals("test", result.getAsString());
  }

  @Test
  void testRequireJsonPrimitive_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireJsonPrimitive("missing"));
  }

  @Test
  void testRequireJsonPrimitive_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireJsonPrimitive("nested"));
  }

  @Test
  void testRequireString_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireString("stringValue");
    assertEquals("test", result);
  }

  @Test
  void testRequireString_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireString("missing"));
  }

  @Test
  void testRequireString_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireString("numberValue"));
  }

  @Test
  void testRequireNumber_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireNumber("numberValue");
    assertNotNull(result);
    assertEquals(42, result.intValue());
  }

  @Test
  void testRequireNumber_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.MissingProperty.class, () -> accessor.requireNumber("missing"));
  }

  @Test
  void testRequireNumber_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireNumber("stringValue"));
  }

  @Test
  void testRequireBool_Valid() throws JsonAccessException {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.requireBool("boolValue");
    assertTrue(result);
  }

  @Test
  void testRequireBool_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(JsonAccessException.MissingProperty.class, () -> accessor.requireBool("missing"));
  }

  @Test
  void testRequireBool_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    assertThrows(
        JsonAccessException.IncorrectType.class, () -> accessor.requireBool("stringValue"));
  }

  @Test
  void testOptionalJsonObject_Valid() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonObject("nested");
    assertNotNull(result);
    assertEquals("nestedTest", result.get("nestedString").getAsString());
  }

  @Test
  void testOptionalJsonObject_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonObject("missing");
    assertNull(result);
  }

  @Test
  void testOptionalJsonObject_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonObject("stringValue");
    assertNull(result);
  }

  @Test
  void testOptionalJsonArray_Valid() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonArray("array");
    assertNotNull(result);
    assertEquals(2, result.size());
  }

  @Test
  void testOptionalJsonArray_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonArray("missing");
    assertNull(result);
  }

  @Test
  void testOptionalJsonArray_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalJsonArray("stringValue");
    assertNull(result);
  }

  @Test
  void testOptionalString_Valid() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalString("stringValue");
    assertEquals("test", result);
  }

  @Test
  void testOptionalString_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalString("missing");
    assertNull(result);
  }

  @Test
  void testOptionalString_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalString("numberValue");
    assertNull(result);
  }

  @Test
  void testOptionalBool_Valid() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalBool("boolValue");
    assertEquals(Boolean.TRUE, result);
  }

  @Test
  void testOptionalBool_Missing() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalBool("missing");
    assertNull(result);
  }

  @Test
  void testOptionalBool_IncorrectType() {
    JsonObject json = createTestJsonObject();
    JsonObjectAccessor accessor = new JsonObjectAccessor(json);

    var result = accessor.optionalBool("stringValue");
    assertNull(result);
  }
}
