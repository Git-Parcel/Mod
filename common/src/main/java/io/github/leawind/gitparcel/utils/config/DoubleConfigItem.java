package io.github.leawind.gitparcel.utils.config;

import org.jspecify.annotations.Nullable;

public final class DoubleConfigItem extends ConfigItem<Double, DoubleConfigItem> {
  public double min = 0;
  public double max = Integer.MAX_VALUE;

  public DoubleConfigItem(String name, String description) {
    super(name, description);
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
