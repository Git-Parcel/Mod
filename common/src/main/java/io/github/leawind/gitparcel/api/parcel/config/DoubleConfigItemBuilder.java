package io.github.leawind.gitparcel.api.parcel.config;

import com.mojang.serialization.Codec;

/**
 * Builder for creating {@link ConfigItem} instances with range validation support.
 *
 * @see ConfigItemBuilder#ofDouble(String)
 */
public class DoubleConfigItemBuilder extends ConfigItemBuilder<Double, DoubleConfigItemBuilder> {

  /**
   * Creates a new builder for double configuration items.
   *
   * @param name the name of the configuration item
   */
  DoubleConfigItemBuilder(String name) {
    super(Codec.DOUBLE, name, 0.0);
  }

  /**
   * Sets a range validation for double values.
   *
   * @param min the minimum value (inclusive)
   * @param max the maximum value (inclusive)
   * @return this builder for chaining
   */
  public DoubleConfigItemBuilder range(double min, double max) {
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
