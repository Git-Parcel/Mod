package io.github.leawind.gitparcel.utils.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

/**
 * Provides convenient access to JSON objects with type safety and error handling.
 */
public class JsonObjectAccessor {
  public JsonObject json;

  public JsonObjectAccessor(JsonObject json) {
    this.json = json;
  }

  /**
   * Retrieves a JSON element at the specified path.
   * @param path the path to the element
   * @return the JSON element at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
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

  /**
   * Retrieves a JSON primitive at the specified path.
   * @param path the path to the primitive
   * @return the JSON primitive at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public JsonPrimitive requireJsonPrimitive(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonPrimitive(requireJsonElement(path));
  }

  /**
   * Retrieves a JSON object at the specified path.
   * @param path the path to the object
   * @return the JSON object at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public JsonObject requireJsonObject(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonObject(requireJsonElement(path));
  }

  /**
   * Retrieves a JSON array at the specified path.
   * @param path the path to the array
   * @return the JSON array at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public JsonArray requireJsonArray(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireJsonArray(requireJsonElement(path));
  }

  /**
   * Retrieves a string value at the specified path.
   * @param path the path to the string
   * @return the string value at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public String requireString(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireString(requireJsonElement(path));
  }

  /**
   * Retrieves a number value at the specified path.
   * @param path the path to the number
   * @return the number value at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public Number requireNumber(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireNumber(requireJsonElement(path));
  }

  /**
   * Retrieves a boolean value at the specified path.
   * @param path the path to the boolean
   * @return the boolean value at the path
   * @throws JsonAccessException.MissingProperty if the property is missing
   * @throws JsonAccessException.IncorrectType if the element has incorrect type
   */
  public boolean requireBool(String... path)
      throws JsonAccessException.MissingProperty, JsonAccessException.IncorrectType {
    return requireBool(requireJsonElement(path));
  }

  /**
   * Retrieves a JSON object at the specified path, or null if not found.
   * @param path the path to the object
   * @return the JSON object at the path, or null if not found
   */
  public @Nullable JsonObject optionalJsonObject(String... path) {
    try {
      return requireJsonObject(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  /**
   * Retrieves a JSON array at the specified path, or null if not found.
   * @param path the path to the array
   * @return the JSON array at the path, or null if not found
   */
  public @Nullable JsonArray optionalJsonArray(String... path) {
    try {
      return requireJsonArray(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  /**
   * Retrieves a string value at the specified path, or null if not found.
   * @param path the path to the string
   * @return the string value at the path, or null if not found
   */
  public @Nullable String optionalString(String... path) {
    try {
      return requireString(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  /**
   * Retrieves a boolean value at the specified path, or null if not found.
   * @param path the path to the boolean
   * @return the boolean value at the path, or null if not found
   */
  public @Nullable Boolean optionalBool(String... path) {
    try {
      return requireBool(path);
    } catch (JsonAccessException e) {
      return null;
    }
  }

  /**
   * Converts a JSON element to a JSON object.
   * @param json the JSON element to convert
   * @return the JSON object
   * @throws JsonAccessException.IncorrectType if the element is not a JSON object
   */
  public static JsonObject requireJsonObject(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonObject();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonObject.class, json.toString());
    }
  }

  /**
   * Converts a JSON element to a JSON array.
   * @param json the JSON element to convert
   * @return the JSON array
   * @throws JsonAccessException.IncorrectType if the element is not a JSON array
   */
  public static JsonArray requireJsonArray(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonArray();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonArray.class, json.toString());
    }
  }

  /**
   * Converts a JSON element to a JSON primitive.
   * @param json the JSON element to convert
   * @return the JSON primitive
   * @throws JsonAccessException.IncorrectType if the element is not a JSON primitive
   */
  public static JsonPrimitive requireJsonPrimitive(JsonElement json)
      throws JsonAccessException.IncorrectType {
    try {
      return json.getAsJsonPrimitive();
    } catch (Throwable e) {
      throw new JsonAccessException.IncorrectType(JsonPrimitive.class, json.toString());
    }
  }

  /**
   * Converts a JSON element to a string.
   * @param json the JSON element to convert
   * @return the string value
   * @throws JsonAccessException.IncorrectType if the element is not a string
   */
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

  /**
   * Converts a JSON element to a number.
   * @param json the JSON element to convert
   * @return the number value
   * @throws JsonAccessException.IncorrectType if the element is not a number
   */
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

  /**
   * Converts a JSON element to a boolean.
   * @param json the JSON element to convert
   * @return the boolean value
   * @throws JsonAccessException.IncorrectType if the element is not a boolean
   */
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
