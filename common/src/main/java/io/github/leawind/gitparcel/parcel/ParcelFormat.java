package io.github.leawind.gitparcel.parcel;

import io.github.leawind.gitparcel.Constants;
import java.io.IOException;
import java.nio.file.Path;

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
}
