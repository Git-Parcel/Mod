package io.github.leawind.gitparcel.parcel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.leawind.gitparcel.utils.config.ConfigItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for parcel format configuration.
 *
 * @param <Self> The type of the configuration class
 */
public abstract class ParcelFormatConfig<Self extends ParcelFormatConfig<Self>> {
  private static final Gson GSON = new Gson();

  protected Map<String, ConfigItem<?, ?>> configItems = new HashMap<>();

  @SuppressWarnings("unchecked")
  private Self self() {
    return (Self) this;
  }

  protected Self register(ConfigItem<?, ?> item) {
    configItems.put(item.name(), item);
    return self();
  }

  public Collection<ConfigItem<?, ?>> listConfigItems() {
    return configItems.values();
  }

  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    for (var item : listConfigItems()) {
      json.add(item.name(), item.toJson());
    }
    return json;
  }

  /**
   * Set values from json element
   *
   * <p>All values will be set.
   *
   * <p>Missing fields will be set to default
   */
  public void setFromJson(JsonObject json) throws IllegalArgumentException {
    for (var item : listConfigItems()) {
      try {
        item.setFromJson(json.get(item.name()));
      } catch (IllegalArgumentException e) {
        item.reset();
      }
    }
  }

  public void setFromJsonFile(Path file)
      throws IOException, JsonSyntaxException, IllegalArgumentException {
    setFromJson(GSON.fromJson(Files.readString(file), JsonObject.class));
  }

  public void resetToDefault() {
    for (var item : listConfigItems()) {
      item.reset();
    }
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
