package io.github.leawind.gitparcel.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public final class DoubleConfigItem extends ConfigItem<Double, DoubleConfigItem> {
  public double min = 0;
  public double max = Integer.MAX_VALUE;

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(get());
  }

  @Override
  public Double fromJson(JsonElement json) throws IllegalArgumentException {
    try {
      return json.getAsJsonPrimitive().getAsDouble();
    } catch (Exception e) {
      throw new IllegalArgumentException("Expected double, but got " + json);
    }
  }

  public DoubleConfigItem(String name) {
    super(name);
    defaultValue(0.0);
  }

  @Override
  public @Nullable String validate(Double value) {
    if (value < min || value > max) {
      return "Value out of range: " + value + ", min: " + min + ", max: " + max;
    }
    return super.validate(value);
  }
}
