package io.github.leawind.gitparcel.storage;

import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.mixin.AccessMinecraftServer;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

public final class WorldStorageManager {
  private static final String DIR_NAME = GitParcelMod.MOD_ID;
  private static final String PARCELS_DIR_NAME = "parcels";

  private static final Map<Path, WeakReference<WorldStorageManager>> CACHE =
      new ConcurrentHashMap<>();

  public static WorldStorageManager getInstance(MinecraftServer server) {
    var directory = getWorldDir(server).normalize();

    WeakReference<WorldStorageManager> ref =
        CACHE.compute(
            directory,
            (key, currentRef) -> {
              if (currentRef != null && currentRef.get() != null) {
                return currentRef;
              }
              return new WeakReference<>(new WorldStorageManager(directory));
            });

    return ref.get();
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
