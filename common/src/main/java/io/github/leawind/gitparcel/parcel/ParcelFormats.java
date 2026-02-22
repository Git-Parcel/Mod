package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.parcel.format.mvp.MvpParcelFormatV0;
import io.github.leawind.gitparcel.parcel.format.structuretemplate.CompressedNbtParcelFormatV0;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ParcelFormats {
  private static final Map<String, Map<Integer, ParcelFormat>> BY_ID = registerAll();

  public static final ParcelFormat MVP_V0 = MvpParcelFormatV0.INSTANCE;
  public static final ParcelFormat COMPRESSED_NBT_V0 = CompressedNbtParcelFormatV0.INSTANCE;

  private static Map<String, Map<Integer, ParcelFormat>> registerAll() {
    Map<String, Map<Integer, ParcelFormat>> registry = new HashMap<>();
    registerFormat(registry, MvpParcelFormatV0.INSTANCE);
    registerFormat(registry, CompressedNbtParcelFormatV0.INSTANCE);
    return registry;
  }

  private static void registerFormat(
      Map<String, Map<Integer, ParcelFormat>> registry, ParcelFormat format) {
    registry.putIfAbsent(format.id, new HashMap<>());
    registry.get(format.id).put(format.version, format);
  }

  public static Map<String, Map<Integer, ParcelFormat>> getAllFormats() {
    return BY_ID;
  }

  /**
   * Get the latest version of parcel format with id
   *
   * @param id Parcel format id
   * @return Latest version of parcel format with id, or null if no version found
   */
  public static @Nullable ParcelFormat of(String id) {
    var versions = BY_ID.get(id);
    if (versions == null) {
      return null;
    }
    var sortedVersions =
        versions.values().stream().sorted((a, b) -> b.version - a.version).toList();
    return sortedVersions.getFirst();
  }

  /**
   * Get parcel format with id and version
   *
   * @param id Parcel format id
   * @param version Parcel format version
   * @return Parcel format with id and version, or null if not found
   */
  public static @Nullable ParcelFormat of(String id, int version) {
    return BY_ID.getOrDefault(id, Map.of()).get(version);
  }
}
