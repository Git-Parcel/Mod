package io.github.leawind.gitparcel.api.parcel;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.api.parcel.config.ConfigItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for parcel format configuration.
 *
 * @param <Self> The type of the configuration class
 */
public abstract class ParcelFormatConfig<Self extends ParcelFormatConfig<Self>> {
  private static final Gson GSON = new Gson();

  protected Map<String, ConfigItem<?>> configItems = new HashMap<>();

  @SuppressWarnings("unchecked")
  private Self self() {
    return (Self) this;
  }

  protected Self register(ConfigItem<?> item) {
    configItems.put(item.name(), item);
    return self();
  }

  public Collection<ConfigItem<?>> listConfigItems() {
    return configItems.values();
  }

  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    for (var item : listConfigItems()) {
      json.add(item.name(), item.encodeStart(JsonOps.INSTANCE).getOrThrow());
    }
    return json;
  }

  /**
   * Set values from json element
   *
   * <p>All values will be set.
   *
   * <p>Missing items will be set to default
   *
   * <p>If an item failed to parse, it will be set to default
   *
   * @throws IllegalStateException if some items failed to parse
   */
  public void setFromJson(JsonObject json) throws IllegalStateException {
    ArrayList<IllegalStateException> errors = new ArrayList<>();
    for (ConfigItem<?> item : listConfigItems()) {
      var ele = json.get(item.name());
      setItemFromJson(item, ele, errors);
    }
    if (!errors.isEmpty()) {
      var msg = errors.stream().map(e -> "  " + e.getMessage()).collect(Collectors.joining("\n"));
      throw new IllegalStateException(
          String.format("%d items failed to parse:\n%s", errors.size(), msg));
    }
  }

  private <T> void setItemFromJson(
      ConfigItem<T> item, JsonElement ele, List<IllegalStateException> errors) {
    if (ele == null) {
      item.reset();
      return;
    }

    try {
      item.set((item.codec().parse(JsonOps.INSTANCE, ele).getOrThrow()));
    } catch (IllegalStateException e) {
      item.reset();
      errors.add(e);
    }
  }

  /**
   * Load values of each item from JSON file
   *
   * @param configFile path to JSON file
   */
  public void load(Path configFile)
      throws IOException, JsonSyntaxException, IllegalArgumentException {
    setFromJson(GSON.fromJson(Files.readString(configFile), JsonObject.class));
  }

  /**
   * Save to the given file path.
   *
   * @param configFile Path to the file. The parent directories will be created if not exist. File
   *     will be overwritten if it already exists.
   * @throws IOException If an I/O error occurs while writing the file
   */
  public void save(Path configFile) throws IOException {
    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, GSON.toJson(toJson()));
  }

  public void resetToDefault() {
    for (var item : listConfigItems()) {
      item.reset();
    }
  }

  public static final class None extends ParcelFormatConfig<None> {}
}
