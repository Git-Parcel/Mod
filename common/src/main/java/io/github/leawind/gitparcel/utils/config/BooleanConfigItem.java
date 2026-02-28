package io.github.leawind.gitparcel.utils.config;

import org.jspecify.annotations.Nullable;

public final class BooleanConfigItem extends ConfigItem<Boolean, BooleanConfigItem> {
  public @Nullable String describeTrue = null;
  public @Nullable String describeFalse = null;

  public BooleanConfigItem(String name, String description) {
    super(name, description);
    defaultValue(false);
  }
}
