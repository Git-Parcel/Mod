package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Optional;

/** JSON parsing and validation helpers for API request bodies. */
final class ApiJson {
  static final Gson GSON = new Gson();

  private ApiJson() {}

  /** Parses a request body into an object; empty bodies yield an empty object. */
  static JsonObject parseObject(byte[] body) {
    if (body == null || body.length == 0) {
      return new JsonObject();
    }
    try {
      var parsed = GSON.fromJson(new String(body, java.nio.charset.StandardCharsets.UTF_8), JsonElement.class);
      if (parsed == null) {
        return new JsonObject();
      }
      if (!parsed.isJsonObject()) {
        throw new ApiException(400, "invalid_body");
      }
      return parsed.getAsJsonObject();
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(400, "invalid_body");
    }
  }

  static String requireString(JsonObject object, String key) {
    var value = optString(object, key);
    if (value.isEmpty()) {
      throw new ApiException(400, "invalid_body");
    }
    return value.get();
  }

  static Optional<String> optString(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || value.isJsonNull()) {
      return Optional.empty();
    }
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
      throw new ApiException(400, "invalid_body");
    }
    return Optional.of(value.getAsString());
  }

  static boolean requireBool(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
      throw new ApiException(400, "invalid_body");
    }
    return value.getAsBoolean();
  }

  static Optional<Boolean> optBool(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || value.isJsonNull()) {
      return Optional.empty();
    }
    return Optional.of(requireBool(object, key));
  }

  /** Reads an {@code [x,y,z]} integer coordinate triple. */
  static int[] requireCoord(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || !value.isJsonArray()) {
      throw new ApiException(400, "invalid_body");
    }
    var array = value.getAsJsonArray();
    if (array.size() != 3) {
      throw new ApiException(400, "invalid_body");
    }
    var result = new int[3];
    for (int i = 0; i < 3; i++) {
      var element = array.get(i);
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
        throw new ApiException(400, "invalid_body");
      }
      result[i] = element.getAsInt();
    }
    return result;
  }

  /** Reads an enum constant by case-insensitive name. */
  static <T extends Enum<T>> T requireEnum(JsonObject object, String key, Class<T> type) {
    var name = requireString(object, key);
    return parseEnum(name, type);
  }

  static <T extends Enum<T>> Optional<T> optEnum(JsonObject object, String key, Class<T> type) {
    return optString(object, key).map(name -> parseEnum(name, type));
  }

  private static <T extends Enum<T>> T parseEnum(String name, Class<T> type) {
    try {
      return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ApiException(400, "invalid_value");
    }
  }

  static JsonArray stringArray(Iterable<String> values) {
    var array = new JsonArray();
    for (var value : values) {
      array.add(value);
    }
    return array;
  }
}
