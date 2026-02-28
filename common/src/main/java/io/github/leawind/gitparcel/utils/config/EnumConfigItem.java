package io.github.leawind.gitparcel.utils.config;

import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class EnumConfigItem<E extends Enum<?>> extends ConfigItem<E, EnumConfigItem<E>> {

  public List<E> values;
  public Function<E, String> describeValue = Enum::name;

  private boolean allowNull = false;

  public EnumConfigItem(String name, String description) {
    super(name, description);
  }

  @Override
  public @Nullable String validate(E value) {
    return !allowNull && value == null ? "Value must be specified" : super.validate(value);
  }

  public boolean allowNull() {
    return allowNull;
  }

  public EnumConfigItem<E> allowNull(boolean allowNull) {
    this.allowNull = allowNull;
    return this;
  }
}
