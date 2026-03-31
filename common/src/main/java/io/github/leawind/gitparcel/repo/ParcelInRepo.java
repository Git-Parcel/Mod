package io.github.leawind.gitparcel.repo;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.utils.git.GitRepo;
import java.nio.file.Path;
import org.slf4j.Logger;

public abstract sealed class ParcelInRepo permits InternalParcelInRepo, CustomParcelInRepo {
  protected static final Logger LOGGER = LogUtils.getLogger();

  protected final GitRepo repo;

  /**
   * Creates a new ParcelRepo instance.
   *
   * @param path The path to the parcel repository directory
   */
  protected ParcelInRepo(Path path) {
    this.repo = GitRepo.get(path);
  }

  public GitRepo getGitRepo() {
    return repo;
  }

  public abstract Path getParcelDir();
}
