package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.Constants;
import io.github.leawind.gitparcel.parcel.format.mvp.MvpParcelFormatV0;
import io.github.leawind.gitparcel.parcel.format.structuretemplate.CompressedNbtParcelFormatV0;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class ParcelFormat {
  public final String id;
  public final int version;

  protected ParcelFormat(String id, int version) {
    this.id = id;
    this.version = version;
  }

  /**
   * Save parcel content to directory
   *
   * @param parcel Parcel to save
   * @param dir Path to directory. recursively create if not exists
   */
  protected abstract void saveContent(Parcel parcel, Path dir) throws IOException;

  /**
   * Load parcel content from directory
   *
   * @param parcel Parcel to load
   * @param dir Path to parcel directory, must exist
   */
  protected abstract void loadContent(Parcel parcel, Path dir) throws IOException;

  public void saveMetadata(Parcel parcel, Path dir) {
    // TODO
    Constants.LOG.warn("Saving metadata of parcel {} to {}", parcel, dir);
  }

  public void loadMetadata(Parcel parcel, Path dir) {
    // TODO
    Constants.LOG.warn("Loading metadata of parcel {} from {}", parcel, dir);
  }

  public void save(Parcel parcel, Path dir) throws IOException {
    Constants.LOG.info("Saving parcel {} to {}", parcel, dir);
    saveMetadata(parcel, dir);
    saveContent(parcel, dir);
  }

  public void load(Parcel parcel, Path dir) throws IOException {
    Constants.LOG.info("Loading parcel {} from {}", parcel, dir);
    loadMetadata(parcel, dir);
    loadContent(parcel, dir);
  }

  /**
   * Get the latest version of parcel format with id
   *
   * @param id Parcel format id
   * @return Latest version of parcel format with id, or null if no version found
   */
  public static @Nullable ParcelFormat of(String id) {
    var versions = Registry.REGISTRY.get(id);
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
    return Registry.REGISTRY.getOrDefault(id, Map.of()).get(version);
  }

  public static final class Registry {
    private static final Map<String, Map<Integer, ParcelFormat>> REGISTRY = registerAll();

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
      registry.getOrDefault(format.id, new HashMap<>()).put(format.version, format);
    }
  }
}
