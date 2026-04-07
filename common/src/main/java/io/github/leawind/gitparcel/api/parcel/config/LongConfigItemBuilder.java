package io.github.leawind.gitparcel.api.parcel.config;

import com.mojang.serialization.Codec;

/**
 * Builder for creating {@link ConfigItem} instances with range validation support.
 *
 * @see ConfigItemBuilder#ofLong(String)
 */
public class LongConfigItemBuilder extends ConfigItemBuilder<Long, LongConfigItemBuilder> {

  /**
   * Creates a new builder for long integer configuration items.
   *
   * @param name the name of the configuration item
   */
  LongConfigItemBuilder(String name) {
    super(Codec.LONG, name, 0L);
  }

  /**
   * Sets a range validation for long values.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return this builder for chaining
   */
  public LongConfigItemBuilder range(long min, long max) {
    this.validator(
        v -> {
          if (v < min || v > max) {
            return "Value out of range: " + v + ", min: " + min + ", max: " + max;
          }
          return null;
        });
    return this;
  }
}
