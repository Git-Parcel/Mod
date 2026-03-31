package io.github.leawind.gitparcel.repo;

import java.nio.file.Path;

public final class InternalParcelInRepo extends ParcelInRepo {
  private static final String PARCEL_DIR_NAME = "parcel";

  private final Path parcelPath;

  public InternalParcelInRepo(Path path) {
    super(path);
    this.parcelPath = path.resolve(PARCEL_DIR_NAME);
  }

  @Override
  public Path getParcelDir() {
    return parcelPath;
  }
}
