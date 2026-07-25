package io.github.leawind.gitparcel.common.minecraft.logic.world;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.world.ParcelValidator;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelRepositoryService;
import io.github.leawind.gitparcel.common.minecraft.logic.storage.ParcelStorage;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.common.utils.git.GitRepo;
import io.github.leawind.gitparcel.server.minecraft.logic.storage.StorageUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Coordinates parcel state, storage, and client synchronization for one server level.
 *
 * <p>{@link Parcel} remains a serializable domain object; all operations that require a live
 * Minecraft level are kept here.
 */
public final class ParcelService {
  private final ServerLevel level;
  private final GitParcelLevelSavedData savedData;

  private ParcelService(ServerLevel level) {
    this.level = level;
    this.savedData = GitParcelLevelSavedData.get(level);
  }

  public static ParcelService get(ServerLevel level) {
    return new ParcelService(level);
  }

  public Collection<Parcel> parcels() {
    return List.copyOf(savedData.parcels().values());
  }

  public @Nullable Parcel getParcel(UUID uuid) {
    return savedData.parcels().get(uuid);
  }

  public void reset() {
    savedData.clearParcels();
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.fullSync(savedData.parcels()));
  }

  public void addNewParcel(Parcel parcel) throws IllegalArgumentException {
    var maxParcelVolume = GitParcelWorldSavedData.get(level.getServer()).maxParcelVolume();
    ParcelValidator.validateNewParcel(parcel, savedData.parcels().values(), maxParcelVolume);

    savedData.addParcel(parcel);
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  public @Nullable Parcel deleteParcel(UUID uuid) {
    var deleted = savedData.removeParcel(uuid);
    if (deleted != null) {
      Services.SERVER_NETWORKING.sendToAllPlayers(
          level, UpdateParcelsMessage.removals(Set.of(uuid)));
    }
    return deleted;
  }

  /** Marks a mutated parcel as dirty and synchronizes it to clients. */
  public void updateParcel(Parcel parcel) {
    if (savedData.parcels().get(parcel.uuid()) != parcel) {
      throw new IllegalArgumentException(
          "Parcel %s is not registered in this level".formatted(parcel.uuid()));
    }

    savedData.setDirty();
    Services.SERVER_NETWORKING.sendToAllPlayers(
        level, UpdateParcelsMessage.incremental(parcel));
  }

  public Path getParcelDirectory(Parcel parcel) {
    return ParcelStorage.resolveParcelDirectory(parcel, getInternalParcelsDirectory());
  }

  public void saveParcel(Parcel parcel, boolean ignoreEntities)
      throws IOException, ParcelException {
    ParcelStorage.save(level, parcel, getParcelDirectory(parcel), ignoreEntities);
  }

  public Optional<GitRepo.CommitInfo> commitParcel(
      Parcel parcel, String message, GitRepo.CommitIdentity identity)
      throws IOException, ParcelException {
    return ParcelRepositoryService.commit(
        parcel, getInternalParcelsDirectory(), message, identity);
  }

  public List<GitRepo.CommitInfo> getParcelHistory(Parcel parcel, int limit)
      throws IOException, ParcelException {
    return ParcelRepositoryService.history(
        parcel, getInternalParcelsDirectory(), limit);
  }

  public void restoreParcel(Parcel parcel, String revision, boolean ignoreEntities)
      throws IOException, ParcelException {
    ParcelRepositoryService.restore(
        level,
        parcel,
        getInternalParcelsDirectory(),
        revision,
        ignoreEntities);
  }

  public void syncTo(ServerPlayer player) {
    Services.SERVER_NETWORKING.send(
        player, UpdateParcelsMessage.fullSync(savedData.parcels()));
  }

  private Path getInternalParcelsDirectory() {
    return StorageUtils.worldStorage(level.getServer()).getInternalParcelsDir();
  }
}
