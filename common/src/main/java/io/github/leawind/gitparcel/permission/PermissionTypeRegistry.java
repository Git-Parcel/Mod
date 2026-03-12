package io.github.leawind.gitparcel.permission;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

public class PermissionTypeRegistry {
  private final Byte2ObjectMap<PermissionType> byId = new Byte2ObjectArrayMap<>();
  private final Object2ObjectMap<String, PermissionType> byName = new Object2ObjectOpenHashMap<>();
  private byte maxId = -1;

  public @Nullable PermissionType byId(int id) {
    return byId.get((byte) id);
  }

  public @Nullable PermissionType byName(String name) {
    return byName.get(name);
  }

  public byte getMaxId() {
    return maxId;
  }

  public PermissionType register(PermissionType type) {
    if (byId.containsKey(type.id())) {
      throw new IllegalArgumentException("id must be unique: " + type.id());
    }

    if (byName.containsKey(type.name())) {
      throw new IllegalArgumentException("name must be unique: " + type.name());
    }

    maxId = (byte) Math.max(maxId, type.id());

    byId.put(type.id(), type);
    byName.put(type.name(), type);
    return type;
  }

  public record Flags(long bits) {
    public static final Codec<Flags> CODEC = Codec.LONG.xmap(Flags::new, Flags::bits);
  }
}
