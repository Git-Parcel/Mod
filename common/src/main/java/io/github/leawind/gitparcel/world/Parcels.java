package io.github.leawind.gitparcel.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Parcels implements Map<UUID, Parcel> {
  public static final Codec<Parcels> CODEC =
      Parcel.CODEC.listOf().xmap(Parcels::new, Parcels::toList);

  private final Object2ObjectMap<UUID, Parcel> map;

  public Parcels() {
    map = new Object2ObjectOpenHashMap<>(4);
  }

  public Parcels(Map<UUID, Parcel> map) {
    this();
    this.map.putAll(map);
  }

  public Parcels(Collection<Parcel> map) {
    this();
    putAll(map);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public @Nullable Parcel get(Object key) {
    if (key instanceof UUID uuid) {
      return map.get(uuid);
    }
    return null;
  }

  /**
   * @deprecated use {@link #put(Parcel)}
   * @throws IllegalArgumentException if the uuid does not match the parcel's uuid
   */
  @Deprecated
  @Override
  public @Nullable Parcel put(UUID uuid, Parcel parcel) throws IllegalArgumentException {
    if (uuid != parcel.uuid()) {
      throw new IllegalArgumentException("Parcel uuid not match");
    }

    return put(parcel);
  }

  public @Nullable Parcel put(Parcel parcel) {
    return map.put(parcel.uuid(), parcel);
  }

  public void putAll(Collection<Parcel> parcels) {
    for (var parcel : parcels) {
      map.put(parcel.uuid(), parcel);
    }
  }

  public @Nullable Parcel remove(UUID uuid) {
    return map.remove(uuid);
  }

  public void removeAll(Collection<UUID> uuids) {
    for (UUID uuid : uuids) {
      map.remove(uuid);
    }
  }

  public void clear() {
    map.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public @Nullable Parcel remove(Object key) {
    return map.remove(key);
  }

  @Override
  public void putAll(@NonNull Map<? extends UUID, ? extends Parcel> m) {
    map.putAll(m);
  }

  @Override
  public @NonNull Set<UUID> keySet() {
    return map.keySet();
  }

  @Override
  public @NonNull Set<Entry<UUID, Parcel>> entrySet() {
    return map.entrySet();
  }

  @Override
  public @NonNull Collection<Parcel> values() {
    return map.values();
  }

  public boolean intersectsAny(BoundingBox boundingBox) {
    for (var parcel : map.values()) {
      if (parcel.getBoundingBox().intersects(boundingBox)) {
        return true;
      }
    }
    return false;
  }

  public @Nullable Parcel getByBlockPos(BlockPos pos) {
    for (var parcel : map.values()) {
      if (parcel.getBoundingBox().isInside(pos)) {
        return parcel;
      }
    }
    return null;
  }

  public List<Parcel> toList() {
    return map.values().stream().toList();
  }

  public static Parcels singleton(Parcel parcel) {
    return new Parcels(Collections.singleton(parcel));
  }
}
