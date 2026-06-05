package io.github.leawind.gitparcel.core.server.storage;

import io.github.leawind.gitparcel.core.GitParcel;
import io.github.leawind.gitparcel.core.mixin.AccessMinecraftServer;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

public final class WorldStorageManager {
  private static final String DIR_NAME = GitParcel.MOD_ID;
  private static final String PARCELS_DIR_NAME = "parcels";

  private static final ConcurrentHashMap<Path, WorldStorageManager> CACHE = new ConcurrentHashMap<>();

  public static WorldStorageManager getInstance(MinecraftServer server) {
    return CACHE.computeIfAbsent(getWorldDir(server).normalize(), WorldStorageManager::new);
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
