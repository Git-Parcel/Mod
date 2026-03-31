package io.github.leawind.gitparcel.repo;

import java.nio.file.Path;

public final class CustomParcelInRepo extends ParcelInRepo {

  private final Path relative;

  public CustomParcelInRepo(Path path, Path relative) {
    super(path);
    this.relative = relative;
  }

  @Override
  public Path getParcelDir() {
    return repo.path().resolve(relative);
  }
}
