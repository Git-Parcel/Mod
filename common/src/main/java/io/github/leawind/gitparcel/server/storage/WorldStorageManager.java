package io.github.leawind.gitparcel.server.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.mixin.AccessMinecraftServer;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.MinecraftServer;

public final class WorldStorageManager {
  private static final String DIR_NAME = GitParcel.MOD_ID;
  private static final String PARCELS_DIR_NAME = "parcels";

  private static final LoadingCache<Path, WorldStorageManager> CACHE =
      Caffeine.newBuilder()
          .maximumSize(32)
          .expireAfterAccess(10, TimeUnit.MINUTES)
          .weakValues()
          .build(WorldStorageManager::new);

  public static WorldStorageManager getInstance(MinecraftServer server) {
    return CACHE.get(getWorldDir(server).normalize());
  }

  private final Path root;

  WorldStorageManager(Path root) {
    this.root = root;
  }

  public Path getRoot() {
    return root;
  }

  /** Internal parcel repositories directory. */
  public Path getInternalParcelsDir() {
    return root.resolve(PARCELS_DIR_NAME);
  }

  public static Path getWorldDir(MinecraftServer server) {
    return ((AccessMinecraftServer) server)
        .getStorageSource()
        .getLevelDirectory()
        .path()
        .resolve(DIR_NAME);
  }
}
