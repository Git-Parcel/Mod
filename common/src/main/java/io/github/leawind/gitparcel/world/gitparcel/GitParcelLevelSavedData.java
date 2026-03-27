package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.GitParcelApi;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class GitParcelLevelSavedData extends SavedData {
  public static final Codec<GitParcelLevelSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(UUIDUtil.STRING_CODEC, Parcel.CODEC)
                          .fieldOf("parcels")
                          .forGetter(GitParcelLevelSavedData::getParcels))
                  .apply(inst, GitParcelLevelSavedData::new));

  public static final SavedDataType<GitParcelLevelSavedData> TYPE =
      new SavedDataType<>("gitparcel_level", GitParcelLevelSavedData::new, CODEC, null);

  private @Nullable ServerLevel level = null;
  private final Map<UUID, Parcel> parcels;

  private GitParcelLevelSavedData() {
    this(new HashMap<>());
  }

  private GitParcelLevelSavedData(Map<UUID, Parcel> parcels) {
    this.parcels = new Object2ObjectOpenHashMap<>(parcels);
    this.parcels.values().forEach(parcel -> parcel.setLevelSavedData(this));
  }

  public Map<UUID, Parcel> getParcels() {
    return parcels;
  }

  public void reset() {
    parcels.clear();

    setDirty();
    emitParcelsUpdate();
  }

  public List<Parcel> listParcels() {
    return new ArrayList<>(parcels.values());
  }

  public Stream<Parcel> streamParcels() {
    return parcels.values().stream();
  }

  /**
   * Adds a new parcel to the saved data.
   *
   * <p>Validation:
   *
   * <ul>
   *   <li>UUID must be unique
   *   <li>Bounding box must not overlap with existing parcels
   * </ul>
   *
   * @param parcel the parcel to add
   * @throws IllegalArgumentException if UUID or bounding box conflicts with existing parcels
   */
  public void addNewParcel(Parcel parcel) throws IllegalArgumentException {
    // Check: unique uuid
    if (parcels.containsKey(parcel.uuid())) {
      throw new IllegalArgumentException(
          "Parcel with uuid %s already exists".formatted(parcel.uuid()));
    }

    // Check: bounding box no overlap
    for (var oldParcel : parcels.values()) {
      if (parcel.getBoundingBox().intersects(oldParcel.getBoundingBox())) {
        throw new IllegalArgumentException(
            "The new parcel intersects with existing parcel: %s <> %s"
                .formatted(parcel.uuid(), oldParcel.uuid()));
      }
    }

    parcel.setLevelSavedData(this);
    parcels.put(parcel.uuid(), parcel);
    setDirty();
    emitParcelsUpdate();
  }

  /**
   * Deletes a parcel by its UUID.
   *
   * @param uuid the UUID of the parcel to delete
   * @return the deleted Parcel object, or null if no parcel with the given UUID exists
   */
  public @Nullable Parcel deleteParcel(UUID uuid) {
    var result = parcels.remove(uuid);

    if (result != null) {
      setDirty();
      emitParcelsUpdate();
    }
    return result;
  }

  public @Nullable Parcel getParcel(UUID uuid) {
    return parcels.get(uuid);
  }

  public @Nullable Parcel getParcel(BlockPos pos) {
    for (var parcel : parcels.values()) {
      if (parcel.getBoundingBox().isInside(pos)) {
        return parcel;
      }
    }
    return null;
  }

  public void emitParcelsUpdate() {
    if (level != null) {
      GitParcelApi.Events.ON_PARCELS_UPDATE.emit(
          new GitParcelApi.Events.UpdateParcelsEvent(level, listParcels()));
    }
  }

  public void emitParcelUpdate(Parcel parcel) {
    if (level != null && parcels.containsKey(parcel.uuid())) {
      GitParcelApi.Events.ON_PARCEL_UPDATE.emit(
          new GitParcelApi.Events.UpdateParcelEvent(level, parcel));
    }
  }

  public static GitParcelLevelSavedData get(ServerLevel level) {
    var savedData = level.getDataStorage().computeIfAbsent(TYPE);
    savedData.level = level;
    return savedData;
  }

  public static void moveParcel(ServerLevel fromLevel, ServerLevel toLevel, UUID uuid) {
    var from = fromLevel.getDataStorage().get(TYPE);
    if (from == null) {
      return;
    }
    var parcel = from.getParcels().remove(uuid);
    var to = toLevel.getDataStorage().computeIfAbsent(TYPE);
    to.getParcels().put(uuid, parcel);
  }
}
