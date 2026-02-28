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
  public abstract static class ConfigItem<Self, T> {
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

  public static class BooleanConfigItem extends ConfigItem<BooleanConfigItem, Boolean> {
    public @Nullable String describeTrue = null;
    public @Nullable String describeFalse = null;

    public BooleanConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static class StringConfigItem extends ConfigItem<StringConfigItem, String> {
    public boolean isLarge = false;
    public @Nullable Function<String, @Nullable String> validator;

    protected StringConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static class LongConfigItem extends ConfigItem<LongConfigItem, Long> {
    public long min = 0;
    public long max = Integer.MAX_VALUE;

    protected LongConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static class DoubleConfigItem extends ConfigItem<DoubleConfigItem, Double> {
    public long min = 0;
    public long max = Integer.MAX_VALUE;

    protected DoubleConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static class EnumConfigItem<E extends Enum<?>> extends ConfigItem<EnumConfigItem<E>, E> {

    public List<E> values;
    public Function<E, String> describeValue = Enum::name;

    protected EnumConfigItem(String name, String description) {
      super(name, description);
    }
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
