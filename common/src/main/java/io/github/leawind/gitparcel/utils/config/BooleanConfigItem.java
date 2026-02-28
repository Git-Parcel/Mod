package io.github.leawind.gitparcel.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public final class BooleanConfigItem extends ConfigItem<Boolean, BooleanConfigItem> {
  public @Nullable String describeTrue = null;
  public @Nullable String describeFalse = null;

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(get());
  }

  @Override
  public Boolean fromJson(JsonElement json) throws IllegalArgumentException {
    try {
      return json.getAsJsonPrimitive().getAsBoolean();
    } catch (Exception e) {
      throw new IllegalArgumentException("Expected boolean, but got " + json);
    }
  }

  public BooleanConfigItem(String name) {
    super(name);
    defaultValue(false);
  }
}
