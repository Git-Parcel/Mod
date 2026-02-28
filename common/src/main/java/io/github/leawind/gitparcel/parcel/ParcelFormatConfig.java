package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.utils.config.ConfigItem;
import java.util.List;

/**
 * Base class for parcel format configuration.
 *
 * @param <C> The type of the configuration class
 */
public abstract class ParcelFormatConfig<C extends ParcelFormatConfig<C>> {

  public C getDefaultConfig() {
    return null;
  }

  public List<ConfigItem<?, ?>> listConfigItems() {
    return List.of();
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
