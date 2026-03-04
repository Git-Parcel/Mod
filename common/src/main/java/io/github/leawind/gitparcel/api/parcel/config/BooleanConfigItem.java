package io.github.leawind.gitparcel.api.parcel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

/** Configuration item for boolean values. */
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

  /**
   * Creates a new boolean configuration item.
   *
   * @param name the name of the configuration item
   */
  public BooleanConfigItem(String name) {
    super(name);
    defaultValue(false);
  }
}
