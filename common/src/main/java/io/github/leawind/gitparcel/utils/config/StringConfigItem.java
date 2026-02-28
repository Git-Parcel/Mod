package io.github.leawind.gitparcel.utils.config;

public final class StringConfigItem extends ConfigItem<String, StringConfigItem> {
  public boolean isLarge = false;

  public StringConfigItem(String name, String description) {
    super(name, description);
    defaultValue("");
  }

  @Override
  public String get() {
    var value = super.get();
    return value == null ? "" : value;
  }
}
