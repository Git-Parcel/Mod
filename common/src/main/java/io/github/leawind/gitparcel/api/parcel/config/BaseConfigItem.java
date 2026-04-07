package io.github.leawind.gitparcel.api.parcel.config;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Base implementation of {@link ConfigItem}.
 *
 * <p>This class provides a complete implementation of a configuration item that can be created
 * directly or via {@link ConfigItemBuilder}.
 *
 * @param <T> The type of the configuration value
 */
public class BaseConfigItem<T> implements ConfigItem<T> {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final Codec<T> codec;
  private final String name;
  private final @Nullable String description;
  private final T defaultValue;
  private final @Nullable Function<T, @Nullable String> validator;
  private final boolean userVisible;

  private final Supplier<T> getter;
  private final Consumer<T> setter;

  /**
   * Creates a new configuration item with all properties.
   *
   * @param name the name of the configuration item
   * @param defaultValue the default value
   * @param codec the codec for serializing/deserializing
   * @param getter the getter function
   * @param setter the setter function
   * @param description the description (can be null)
   * @param validator the validator function (can be null)
   * @param userVisible whether this item is visible to users
   */
  public BaseConfigItem(
      String name,
      T defaultValue,
      Codec<T> codec,
      Supplier<T> getter,
      Consumer<T> setter,
      @Nullable String description,
      @Nullable Function<T, @Nullable String> validator,
      boolean userVisible) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.codec = codec;
    this.getter = getter;
    this.setter = setter;
    this.description = description;
    this.validator = validator;
    this.userVisible = userVisible;
  }

  @Override
  public Codec<T> codec() {
    return codec;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public @Nullable String description() {
    return description;
  }

  @Override
  public T defaultValue() {
    return defaultValue;
  }

  @Override
  public T get() {
    return getter.get();
  }

  @Override
  public void set(T value) {
    setter.accept(value);
  }

  @Override
  public @Nullable String validate(T value) {
    if (validator == null) {
      return ConfigItem.super.validate(value);
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

  /**
   * Checks if this configuration item is visible to users.
   *
   * @return true if the item is user-visible
   */
  public boolean userVisible() {
    return userVisible;
  }
}
