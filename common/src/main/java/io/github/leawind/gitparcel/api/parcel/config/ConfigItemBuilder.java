package io.github.leawind.gitparcel.api.parcel.config;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Builder for creating {@link ConfigItem} instances with a fluent API.
 *
 * <p><b>Notes:</b>
 *
 * <ul>
 *   <li>If neither getter nor setter is set, {@link #build()} auto-calls {@link #storeLocally()};
 *   <li>This class is not thread-safe; validators should not throw exceptions
 *   <li>Builder state is not reset after {@link #build()}, allowing reuse for shared-config items
 *   <li>{@link #validator(Function)} overwrites any previous validator
 * </ul>
 *
 * @param <T> the type of the configuration value
 * @see ConfigItem
 * @see BaseConfigItem
 */
public class ConfigItemBuilder<T, Self extends ConfigItemBuilder<T, Self>> {
  private final Codec<T> codec;
  private final String name;
  private T defaultValue;
  private @Nullable String description = null;
  private @Nullable Function<T, @Nullable String> validator = null;
  private boolean userVisible = true;
  private @Nullable Supplier<T> getter = null;
  private @Nullable Consumer<T> setter = null;

  @SuppressWarnings("unchecked")
  private Self self() {
    return (Self) this;
  }

  /**
   * Creates a new builder with the specified codec and default value.
   *
   * @param codec the codec for serializing/deserializing the value
   * @param name the name of the configuration item
   * @param defaultValue the default value (must not be null)
   */
  public ConfigItemBuilder(Codec<T> codec, String name, T defaultValue) {
    this.codec = codec;
    this.name = name;
    this.defaultValue = defaultValue;
  }

  /**
   * Sets the default value of this configuration item.
   *
   * @param defaultValue the default value (must not be null)
   * @return this builder for chaining
   */
  public Self defaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
    return self();
  }

  /**
   * Sets the description of this configuration item.
   *
   * @param description the description text
   * @return this builder for chaining
   */
  public Self description(@Nullable String description) {
    this.description = description;
    return self();
  }

  /**
   * Sets the validator function for this configuration item.
   *
   * <ul>
   *   <li>If the validator function is null, all values are considered valid.
   *   <li>If the validator function returns a non-null string, the value is considered invalid and
   *       the string is used as the error message.
   *   <li>If the validator function returns null, the value is considered valid.
   *   <li>It should never throw an exception.
   * </ul>
   *
   * @param validator the validator function
   * @return this builder for chaining
   */
  public Self validator(@Nullable Function<T, @Nullable String> validator) {
    this.validator = validator;
    return self();
  }

  /**
   * Sets whether this configuration item is visible to users.
   *
   * @param userVisible true to make the item user-visible
   * @return this builder for chaining
   */
  public Self userVisible(boolean userVisible) {
    this.userVisible = userVisible;
    return self();
  }

  /**
   * Sets the getter function for this configuration item.
   *
   * @param getter the getter function
   * @return this builder for chaining
   */
  public Self getter(Supplier<T> getter) {
    this.getter = getter;
    return self();
  }

  /**
   * Sets the setter function for this configuration item.
   *
   * @param setter the setter function
   * @return this builder for chaining
   */
  public Self setter(Consumer<T> setter) {
    this.setter = setter;
    return self();
  }

  /**
   * Configures this item to store its value locally (not backed by external storage).
   *
   * <p>This will call {@link #getter(Supplier)} and {@link #setter(Consumer)} with a {@link Box}
   * instance that stores the value.
   *
   * @return this builder for chaining
   */
  public Self storeLocally() {
    var defaultValue = this.defaultValue;
    var box = new Box<>(defaultValue);
    getter(box::get);
    setter(box::set);
    return self();
  }

  /**
   * Builds the configuration item.
   *
   * @return a new {@link ConfigItem} instance
   */
  public ConfigItem<T> build() {
    if (getter == null && setter == null) {
      storeLocally();
    }
    // getter and setter are guaranteed non-null after storeLocally()
    return new BaseConfigItem<>(
        name, defaultValue, codec, getter, setter, description, validator, userVisible);
  }

  /**
   * Creates a builder for boolean configuration items with default value {@code false}.
   *
   * @param name the name of the configuration item
   * @return a new builder for boolean values
   */
  public static ConfigItemBuilder<Boolean, ?> ofBoolean(String name) {
    return new ConfigItemBuilder<>(Codec.BOOL, name, false);
  }

  /**
   * Creates a builder for long integer configuration items with default value {@code 0L}.
   *
   * @param name the name of the configuration item
   * @return a new builder for long values with range validation support
   */
  public static LongConfigItemBuilder ofLong(String name) {
    return new LongConfigItemBuilder(name);
  }

  /**
   * Creates a builder for double configuration items with default value {@code 0.0}.
   *
   * @param name the name of the configuration item
   * @return a new builder for double values with range validation support
   */
  public static DoubleConfigItemBuilder ofDouble(String name) {
    return new DoubleConfigItemBuilder(name);
  }

  /**
   * Creates a builder for string configuration items with default value {@code ""}.
   *
   * @param name the name of the configuration item
   * @return a new builder for string values
   */
  public static ConfigItemBuilder<String, ?> ofString(String name) {
    return new ConfigItemBuilder<>(Codec.STRING, name, "");
  }

  /**
   * Creates a builder for enum configuration items.
   *
   * @param name the name of the configuration item
   * @param defaultValue the default value of the enum item (must not be null)
   * @return a new builder for enum values
   * @param <E> the type of the enum
   */
  public static <E extends Enum<E>> ConfigItemBuilder<E, ?> ofEnum(String name, E defaultValue) {
    Class<E> enumClass = defaultValue.getDeclaringClass();
    return new ConfigItemBuilder<>(
        Codec.STRING.xmap(
            s -> {
              for (E constant : enumClass.getEnumConstants()) {
                if (constant.name().equals(s)) {
                  return constant;
                }
              }
              throw new IllegalArgumentException("Unrecognized enum value: " + s);
            },
            Enum::name),
        name,
        defaultValue);
  }

  /**
   * Simple box class for storing values locally.
   *
   * @param <T> the type of the value
   */
  private static class Box<T> {
    private T value;

    Box(T value) {
      this.value = value;
    }

    T get() {
      return value;
    }

    void set(T value) {
      this.value = value;
    }
  }
}
