package io.github.leawind.gitparcel.parcel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    for (var configItem : listConfigItems()) {
      json.add(configItem.name(), configItem.toJson());
    }
    return json;
  }

  public void setFromJson(JsonElement jsonElement) throws IllegalArgumentException {
    if (jsonElement instanceof JsonObject json) {
      for (var item : listConfigItems()) {
        try {
          item.setFromJson(json.get(item.name()));
        } catch (IllegalArgumentException e) {
          item.reset();
        }
      }
    } else {
      throw new IllegalArgumentException("Expected JsonObject, but got " + jsonElement.getClass());
    }
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
