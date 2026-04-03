package io.github.leawind.gitparcel.api.parcel.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class EnumConfigItem<E extends Enum<?>> extends ConfigItem<E, EnumConfigItem<E>> {

  public List<E> values;
  public Function<E, String> describeValue = Enum::name;

  private boolean allowNull = false;

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(get().name());
  }

  @Override
  public E fromJson(JsonElement json) throws IllegalArgumentException {
    String name;
    try {
      name = json.getAsJsonPrimitive().getAsString();
    } catch (Exception e) {
      throw new IllegalArgumentException("Expected string, but got " + json);
    }
    for (var value : values) {
      if (value.name().equals(name)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unrecognized enum value: " + name);
  }

  public EnumConfigItem(Class<E> enumClass, String name) {
    super(name);
    values = List.of(enumClass.getEnumConstants());
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
