package io.github.leawind.gitparcel.utils.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class JsonObjectAccessor {
  public JsonObject json;

  public JsonObjectAccessor(JsonObject json) {
    this.json = json;
  }

  public JsonElement requireJsonElement(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    JsonElement element = json;
    String lastKey = null;
    for (String key : path) {
      if (!element.isJsonObject()) {
        throw new JsonAccessException.IncorrectType("JsonObject", element.toString());
      }
      element = element.getAsJsonObject().get(key);
      lastKey = key;
    }
    if (element == null) {
      throw new JsonAccessException.MissingProperty(lastKey);
    }
    return element;
  }

  public JsonPrimitive requireJsonPrimitive(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonPrimitive(requireJsonElement(path));
  }

  public JsonObject requireJsonObject(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonObject(requireJsonElement(path));
  }

  public JsonArray requireJsonArray(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonArray(requireJsonElement(path));
  }

  public String requireString(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireString(requireJsonElement(path));
  }

  public Number requireNumber(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireNumber(requireJsonElement(path));
  }

  public boolean requireBool(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireBool(requireJsonElement(path));
  }

  public @Nullable JsonObject optionalJsonObject(String... path) {
    try {
      return requireJsonObject(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  public @Nullable JsonArray optionalJsonArray(String... path) {
    try {
      return requireJsonArray(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  public @Nullable String optionalString(String... path) {
    try {
      return requireString(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  public @Nullable Boolean optionalBool(String... path) {
    try {
      return requireBool(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  public static JsonObject requireJsonObject(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonObject();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonObject.class, json.toString());
    }
  }

  public static JsonArray requireJsonArray(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonArray();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonArray.class, json.toString());
    }
  }

  public static JsonPrimitive requireJsonPrimitive(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonPrimitive();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonPrimitive.class, json.toString());
    }
  }

  public static String requireString(JsonElement json) throws JsonAccessException.IncorrectType {
    try {
      var primitive = json.getAsJsonPrimitive();
      if (!primitive.isString()) {
        throw new AssertionError();
      }
      return primitive.getAsString();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(String.class, json.toString());
    }
  }

  public static Number requireNumber(JsonElement json) throws JsonAccessException.IncorrectType {
    try {
      var primitive = json.getAsJsonPrimitive();
      if (!primitive.isNumber()) {
        throw new AssertionError();
      }
      return primitive.getAsNumber();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(Number.class, json.toString());
    }
  }

  public static boolean requireBool(JsonElement json) throws JsonAccessException.IncorrectType {
    try {
      var primitive = json.getAsJsonPrimitive();
      if (!primitive.isBoolean()) {
        throw new AssertionError();
      }
      return primitive.getAsBoolean();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(Boolean.class, json.toString());
    }
  }
}
