package io.github.leawind.gitparcel.server.storage;

import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.systemstoragelib.v1.api.Scope;
import io.github.leawind.systemstoragelib.v1.api.StoreType;
import io.github.leawind.systemstoragelib.v1.api.SystemStorageLib;
import io.github.leawind.systemstoragelib.v1.api.accessors.SecretsAccessor;
import java.nio.file.Path;

public class SystemStorageManager {
  private static final Scope SCOPE = SystemStorageLib.getInstance().scope(GitParcel.MOD_ID);

  public static Path getDataDir() {
    return SCOPE.directory(StoreType.DATA);
  }

  public static Path getCacheDir() {
    return SCOPE.directory(StoreType.CACHE);
  }

  public static SecretsAccessor getSecrets() {
    return SCOPE.access(StoreType.SECRETS, SecretsAccessor::from);
  }
}
