package io.github.leawind.gitparcel.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.network.protocol.parcels.UpdateParcelsS2CPayload;
import io.github.leawind.gitparcel.utils.NetworkUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public final class GitParcelLevelSavedData extends SavedData {
  public static final Codec<GitParcelLevelSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Parcels.CODEC.fieldOf("parcels").forGetter(GitParcelLevelSavedData::parcels))
                  .apply(inst, GitParcelLevelSavedData::new));

  public static final SavedDataType<GitParcelLevelSavedData> TYPE =
      new SavedDataType<>("gitparcel_level", GitParcelLevelSavedData::new, CODEC, null);

  /**
   * Set when the saved data is loaded from the level.
   *
   * @see #get(ServerLevel)
   */
  private @Nullable ServerLevel level = null;

  private final Parcels parcels;

  private GitParcelLevelSavedData() {
    this(new Parcels());
  }

  private GitParcelLevelSavedData(Parcels parcels) {
    this.parcels = parcels;
    parcels.values().forEach(parcel -> parcel.setLevelSavedData(this));
  }

  public Parcels parcels() {
    return parcels;
  }

  public void reset() {
    parcels.clear();

    setDirty();
    if (level != null) {
      var payload = UpdateParcelsS2CPayload.fullSync(parcels);
      NetworkUtils.sendToAllPlayers(level, payload);
    }
  }

  /**
   * Validation:
   *
   * <ul>
   *   <li>UUID must be unique
   *   <li>Bounding box must not overlap with existing parcels
   * </ul>
   *
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
    parcels.put(parcel);
    setDirty();
    emitParcelUpdate(parcel);
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
      emitParcelsDeleted(uuid);
    }
    return result;
  }

  public @Nullable Parcel getParcel(UUID uuid) {
    return parcels.get(uuid);
  }

  public void emitParcelUpdate(Parcel parcel) {
    if (level != null) {
      var payload = UpdateParcelsS2CPayload.incremental(parcel);
      NetworkUtils.sendToAllPlayers(level, payload);
    }
  }

  public void emitParcelsDeleted(UUID uuid) {
    if (level != null) {
      var payload = UpdateParcelsS2CPayload.removals(List.of(uuid));
      NetworkUtils.sendToAllPlayers(level, payload);
    }
  }

  public @Nullable ServerLevel getLevel() {
    return level;
  }

  public static GitParcelLevelSavedData get(ServerLevel level) {
    var savedData = level.getDataStorage().computeIfAbsent(TYPE);
    savedData.level = level;
    return savedData;
  }
}
