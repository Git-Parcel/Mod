package io.github.leawind.gitparcel.api.parcel.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import org.jspecify.annotations.Nullable;

public interface ConfigItem<T> {
  Codec<T> codec();

  T get();

  void set(T value);

  /**
   * Gets the key of this configuration item.
   *
   * @return the configuration item key
   */
  String name();

  /**
   * Gets the description of this configuration item.
   *
   * @return the configuration item description
   */
  @Nullable String description();

  /**
   * Gets the default value of this configuration item.
   *
   * @return the default value
   */
  T defaultValue();

  default @Nullable String validate(T value) {
    return null;
  }

  /** Resets the configuration item to its default value. */
  default void reset() {
    set(defaultValue());
  }

  /**
   * Checks if this configuration item is visible to users.
   *
   * @return true if the item is user-visible
   */
  default boolean userVisible() {
    return true;
  }

  default <U> DataResult<U> encodeStart(DynamicOps<U> ops) {
    return codec().encodeStart(ops, get());
  }
}
