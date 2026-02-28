package io.github.leawind.gitparcel.utils.config;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Base class for configuration items.
 *
 * @param <T> The type of the configuration value
 */
public abstract sealed class ConfigItem<T, Self extends ConfigItem<T, Self>>
    permits StringConfigItem, BooleanConfigItem, LongConfigItem, DoubleConfigItem, EnumConfigItem {
  private class Box {
    private T value;

    public Box(T value) {
      this.value = value;
    }

    public T get() {
      return value;
    }

    public void set(T value) {
      this.value = value;
    }
  }

  private static final Logger LOGGER = LogUtils.getLogger();

  private final String name;
  private @Nullable String description = null;

  private T defaultValue = null;

  private @Nullable Supplier<T> getter = null;
  private @Nullable Consumer<T> setter = null;

  private @Nullable Function<T, @Nullable String> validator = null;

  private boolean userVisible = true;

  public abstract JsonElement toJson();

  /**
   * Parse json element to value
   *
   * <p>Note: This method does not validate the value
   *
   * @param json json element to parse
   * @return parsed value
   * @throws IllegalArgumentException if json element is invalid
   * @see #setFromJson(JsonElement)
   */
  public abstract T fromJson(JsonElement json) throws IllegalArgumentException;

  /**
   * Parse json element and set value
   *
   * @param json json element to parse
   * @throws IllegalArgumentException if json element is invalid
   */
  public void setFromJson(JsonElement json) throws IllegalArgumentException {
    set(fromJson(json));
  }

  protected ConfigItem(String name) {
    this.name = name;
  }

  @SuppressWarnings("unchecked")
  private Self self() {
    return (Self) this;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public T defaultValue() {
    return defaultValue;
  }

  public void reset() {
    set(defaultValue);
  }

  public T get() {
    return Objects.requireNonNull(getter).get();
  }

  /**
   * Sets the configuration value.
   *
   * <p>Note: This method does not validate the value
   *
   * @param value The value to set
   */
  public void set(T value) {
    Objects.requireNonNull(setter).accept(value);
  }

  /**
   * Validates the configuration value.
   *
   * <p>This function should never throw an exception.
   *
   * @param value The configuration value to validate
   * @return A non-null string if the value is invalid, or null if the value is valid
   */
  public @Nullable String validate(T value) {
    if (validator == null) {
      return null;
    }
    try {
      return validator.apply(value);
    } catch (Exception e) {
      LOGGER.error(
          "Validator threw an exception while validating value {}: {}", value, e.getMessage(), e);
      LOGGER.error("The error message will be used as the validation result");
      return "Validator error: " + e.getMessage();
    }
  }

  public boolean userVisible() {
    return userVisible;
  }

  public Self storeRightHere() {
    var box = new Box(defaultValue());
    this.getter = box::get;
    this.setter = box::set;
    return self();
  }

  public Self description(@Nullable String description) {
    this.description = description;
    return self();
  }

  public Self getter(Supplier<T> getter) {
    this.getter = getter;
    return self();
  }

  public Self setter(Consumer<T> setter) {
    this.setter = setter;
    return self();
  }

  public Self defaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    return self();
  }

  /**
   * Set the validator function for the configuration value.
   *
   * <ul>
   *   <li>If the validator function is null, all values are considered valid.
   *   <li>If the validator function returns a non-null string, the value is considered invalid and
   *       the string is used as the error message.
   *   <li>If the validator function returns null, the value is considered valid.
   *   <li>It should never throw an exception.
   * </ul>
   */
  public Self validator(Function<T, @Nullable String> validator) {
    this.validator = validator;
    return self();
  }

  public Self userVisible(boolean value) {
    userVisible = value;
    return self();
  }
}
