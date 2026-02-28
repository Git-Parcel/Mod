package io.github.leawind.gitparcel.utils.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public final class LongConfigItem extends ConfigItem<Long, LongConfigItem> {
  public long min = 0;
  public long max = Integer.MAX_VALUE;

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(get());
  }

  @Override
  public Long fromJson(JsonElement json) throws IllegalArgumentException {
    try {
      return json.getAsJsonPrimitive().getAsLong();
    } catch (Exception e) {
      throw new IllegalArgumentException("Expected long, but got " + json);
    }
  }

  public LongConfigItem(String name) {
    super(name);
    defaultValue(0L);
  }

  @Override
  public @Nullable String validate(Long value) {
    if (value < min || value > max) {
      return "Value out of range: " + value + ", min: " + min + ", max: " + max;
    }
    return super.validate(value);
  }
}
