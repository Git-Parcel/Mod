package io.github.leawind.gitparcel.utils.config;

import org.jspecify.annotations.Nullable;

public final class LongConfigItem extends ConfigItem<Long, LongConfigItem> {
  public long min = 0;
  public long max = Integer.MAX_VALUE;

  public LongConfigItem(String name, String description) {
    super(name, description);
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
