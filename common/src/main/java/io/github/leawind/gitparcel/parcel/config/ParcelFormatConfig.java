package io.github.leawind.gitparcel.parcel.config;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

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

  /**
   * Base class for configuration items.
   *
   * @param <T> The type of the configuration value
   */
  public abstract static sealed class ConfigItem<T, Self extends ConfigItem<?, ?>>
      permits StringConfigItem,
          BooleanConfigItem,
          LongConfigItem,
          DoubleConfigItem,
          EnumConfigItem {
    protected String name;
    protected String description;
    protected T defaultValue;

    protected Supplier<T> getter;
    protected Consumer<T> setter;

    protected ConfigItem(String name, String description) {
      this.name = name;
      this.description = description;
    }

    @SuppressWarnings("unchecked")
    private Self self() {
      return (Self) this;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public T getDefaultValue() {
      return defaultValue;
    }

    public T get() {
      return getter.get();
    }

    public void set(T value) {
      setter.accept(value);
    }

    public Self getter(Supplier<T> getter) {
      this.getter = getter;
      return self();
    }

    public Self setter(Consumer<T> setter) {
      this.setter = setter;
      return self();
    }
  }

  public static final class BooleanConfigItem extends ConfigItem<Boolean, BooleanConfigItem> {
    public @Nullable String describeTrue = null;
    public @Nullable String describeFalse = null;

    public BooleanConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class StringConfigItem extends ConfigItem<String, StringConfigItem> {
    public boolean isLarge = false;
    public @Nullable Function<String, @Nullable String> validator = null;

    public StringConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class LongConfigItem extends ConfigItem<Long, LongConfigItem> {
    public long min = 0;
    public long max = Integer.MAX_VALUE;

    public LongConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class DoubleConfigItem extends ConfigItem<Double, DoubleConfigItem> {
    public double min = 0;
    public double max = Integer.MAX_VALUE;

    public DoubleConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class EnumConfigItem<E extends Enum<?>>
      extends ConfigItem<E, EnumConfigItem<E>> {

    public List<E> values;
    public Function<E, String> describeValue = Enum::name;

    public EnumConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
