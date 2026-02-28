package io.github.leawind.gitparcel.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringConfigItem extends ConfigItem<String, StringConfigItem> {
  public boolean isLarge = false;

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(get());
  }

  @Override
  public String fromJson(JsonElement json) throws IllegalArgumentException {
    try {
      return json.getAsJsonPrimitive().getAsString();
    } catch (Exception e) {
      throw new IllegalArgumentException("Expected string, but got " + json);
    }
  }

  public StringConfigItem(String name) {
    super(name);
    defaultValue("");
  }

  @Override
  public String get() {
    var value = super.get();
    return value == null ? "" : value;
  }
}
