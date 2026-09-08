package io.github.leawind.gitparcel.server.minecraft.logic.world;

import com.google.common.collect.MapMaker;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.impl.world.ParcelValidator;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelLevelSavedData;
import io.github.leawind.gitparcel.common.minecraft.logic.world.GitParcelWorldSavedData;
import io.github.leawind.gitparcel.server.minecraft.logic.network.ParcelSynchronization;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Server-authoritative registration of parcels in one level.
 *
 * <p>Owns the registration lifecycle (create, update, delete) and the per-parcel use-case locks.
 * Registration changes are broadcast to clients through {@link ParcelSynchronization}. Snapshot
 * and exchange use cases live in {@link SnapshotService} and {@link PublishImportService}.
 */
public final class ParcelRegistry {
  private static final Map<ServerLevel, ParcelRegistry> INSTANCES =
      new MapMaker().weakKeys().makeMap();

  /**
   * One lock per parcel UUID, held only while an operation is in flight. Weak values let entries
   * for deleted parcels be collected instead of accumulating for the server's lifetime.
   */
  private static final ConcurrentMap<UUID, ReentrantLock> PARCEL_LOCKS =
      new MapMaker().weakValues().makeMap();

  public static ParcelRegistry get(ServerLevel level) {
    return INSTANCES.computeIfAbsent(level, ParcelRegistry::new);
  }

  private final ServerLevel level;
  private final GitParcelLevelSavedData savedData;

  private ParcelRegistry(ServerLevel level) {
    this.level = level;
    this.savedData = GitParcelLevelSavedData.get(level);
    repairDimensions();
  }

  /** Backfills the owning dimension once per loaded level instead of on every command. */
  private void repairDimensions() {
    String dimension = level.dimension().identifier().toString();
    boolean changed = false;
    for (Parcel parcel : savedData.parcels().values()) {
      if (parcel.dimension().isEmpty()) {
        parcel.assignDimension(dimension);
        changed = true;
      }
    }
    if (changed) {
      savedData.setDirty();
    }
  }

  public Collection<Parcel> parcels() {
    return List.copyOf(savedData.parcels().values());
  }

  public io.github.leawind.gitparcel.common.api.world.Parcels parcelsById() {
    return new io.github.leawind.gitparcel.common.api.world.Parcels(savedData.parcels());
  }

  public @Nullable Parcel getParcel(UUID uuid) {
    return savedData.parcels().get(uuid);
  }

  public void reset() {
    savedData.clearParcels();
    ParcelSynchronization.broadcastFullSync(level, parcelsById());
  }

  public void addNewParcel(Parcel parcel) throws IllegalArgumentException {
    parcel.assignDimension(level.dimension().identifier().toString());
    validateNewParcel(parcel);
    savedData.addParcel(parcel);
    ParcelSynchronization.broadcastIncremental(level, parcel);
  }

  public void validateNewParcel(Parcel parcel) throws IllegalArgumentException {
    var maxParcelVolume = GitParcelWorldSavedData.get(level.getServer()).maxParcelVolume();
    ParcelValidator.validateNewParcel(parcel, savedData.parcels().values(), maxParcelVolume);
  }

  /** Removes only the world registration; the repository is deliberately retained. */
  public @Nullable Parcel deleteParcel(UUID uuid) throws ParcelException {
    ReentrantLock lock = acquireParcelLock(uuid);
    try {
      var deleted = savedData.removeParcel(uuid);
      if (deleted != null) {
        ParcelSynchronization.broadcastRemovals(level, Set.of(uuid));
      }
      return deleted;
    } finally {
      lock.unlock();
    }
  }

  /** Atomically validates and unregisters a batch while acquiring locks in UUID order. */
  public int deleteParcels(Collection<Parcel> parcels) throws ParcelException {
    var targets =
        parcels.stream()
            .distinct()
            .sorted(Comparator.comparing(parcel -> parcel.uuid().toString()))
            .toList();
    var locks = new ArrayList<ReentrantLock>(targets.size());
    try {
      for (Parcel parcel : targets) {
        locks.add(acquireParcelLock(parcel.uuid()));
      }
      for (Parcel parcel : targets) {
        requireRegistered(parcel);
      }
      var removed = new HashSet<UUID>();
      for (Parcel parcel : targets) {
        if (savedData.removeParcel(parcel.uuid()) != null) {
          removed.add(parcel.uuid());
        }
      }
      if (!removed.isEmpty()) {
        ParcelSynchronization.broadcastRemovals(level, Set.copyOf(removed));
      }
      return removed.size();
    } finally {
      for (int i = locks.size() - 1; i >= 0; i--) {
        locks.get(i).unlock();
      }
    }
  }

  /** Marks a mutated parcel as dirty and synchronizes runtime properties to clients. */
  public void updateParcel(Parcel parcel) {
    requireRegistered(parcel);
    savedData.setDirty();
    ParcelSynchronization.broadcastIncremental(level, parcel);
  }

  /** Serializes mutating use cases for one parcel; queries do not take this lock. */
  ReentrantLock acquireParcelLock(UUID parcelUuid) throws ParcelException.Busy {
    ReentrantLock lock = PARCEL_LOCKS.computeIfAbsent(parcelUuid, ignored -> new ReentrantLock());
    if (!lock.tryLock()) {
      throw new ParcelException.Busy("Parcel operation is already in progress: " + parcelUuid);
    }
    return lock;
  }

  void requireRegistered(Parcel parcel) {
    if (savedData.parcels().get(parcel.uuid()) != parcel) {
      throw new IllegalArgumentException(
          "Parcel %s is not registered in this level".formatted(parcel.uuid()));
    }
  }
}
