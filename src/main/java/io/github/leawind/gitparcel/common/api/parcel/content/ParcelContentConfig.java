package io.github.leawind.gitparcel.common.api.parcel.content;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import io.github.leawind.gitparcel.common.api.config.ConfigItem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Base class for the per-parcel configuration of one {@link ParcelContentType}. */
public abstract class ParcelContentConfig<Self extends ParcelContentConfig<Self>> {
  private static final Gson GSON = new Gson();

  private final Map<String, ConfigItem<?>> configItems = new LinkedHashMap<>();

  @SuppressWarnings("unchecked")
  private Self self() {
    return (Self) this;
  }

  protected final Self register(ConfigItem<?> item) {
    if (configItems.putIfAbsent(item.name(), item) != null) {
      throw new IllegalArgumentException("Duplicate parcel content config item: " + item.name());
    }
    return self();
  }

  public final Collection<ConfigItem<?>> listConfigItems() {
    return List.copyOf(configItems.values());
  }

  public final JsonObject toJson() {
    JsonObject json = new JsonObject();
    for (var item : configItems.values()) {
      json.add(item.name(), item.encodeStart(JsonOps.INSTANCE).getOrThrow());
    }
    return json;
  }

  /** Replaces all values from JSON, resetting missing values to their defaults. */
  public final void setFromJson(JsonObject json) {
    List<IllegalStateException> errors = new ArrayList<>();
    for (ConfigItem<?> item : configItems.values()) {
      setItemFromJson(item, json.get(item.name()), errors);
    }
    if (!errors.isEmpty()) {
      String message =
          errors.stream().map(error -> "  " + error.getMessage()).collect(Collectors.joining("\n"));
      throw new IllegalStateException(
          "%d items failed to parse:\n%s".formatted(errors.size(), message));
    }
  }

  private static <T> void setItemFromJson(
      ConfigItem<T> item, JsonElement value, List<IllegalStateException> errors) {
    if (value == null) {
      item.reset();
      return;
    }
    try {
      item.set(item.codec().parse(JsonOps.INSTANCE, value).getOrThrow());
    } catch (IllegalStateException e) {
      item.reset();
      errors.add(e);
    }
  }

  public final void load(Path file)
      throws IOException, JsonSyntaxException, IllegalArgumentException {
    setFromJson(GSON.fromJson(Files.readString(file), JsonObject.class));
  }

  public final void save(Path file) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, GSON.toJson(toJson()));
  }

  public final void resetToDefault() {
    configItems.values().forEach(ConfigItem::reset);
  }

  public static final class None extends ParcelContentConfig<None> {}
}
