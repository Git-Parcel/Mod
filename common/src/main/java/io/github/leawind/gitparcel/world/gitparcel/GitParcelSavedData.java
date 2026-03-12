package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class GitParcelSavedData extends SavedData {
  public static final Codec<GitParcelSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.unboundedMap(UUIDUtil.STRING_CODEC, ParcelInstance.CODEC)
                          .fieldOf("parcel_instances")
                          .forGetter(GitParcelSavedData::getParcelInstances))
                  .apply(inst, GitParcelSavedData::new));

  public static final SavedDataType<GitParcelSavedData> TYPE =
      new SavedDataType<>("gitparcel", GitParcelSavedData::new, CODEC, null);

  private final Map<UUID, ParcelInstance> parcelInstances;

  private GitParcelSavedData() {
    this(new HashMap<>());
  }

  private GitParcelSavedData(Map<UUID, ParcelInstance> parcelInstance) {
    this.parcelInstances = parcelInstance;
  }

  private Map<UUID, ParcelInstance> getParcelInstances() {
    return parcelInstances;
  }

  public List<ParcelInstance> listParcelInstances() {
    return new ArrayList<>(parcelInstances.values());
  }

  /**
   * Adds a new parcel instance to the saved data.
   *
   * <p>Validation:
   *
   * <ul>
   *   <li>UUID must be unique
   *   <li>Bounding box must not overlap with existing parcel instances
   * </ul>
   *
   * @param inst the parcel instance to add
   * @throws IllegalArgumentException if UUID or bounding box conflicts with existing parcel
   *     instances
   */
  public void addNewParcelInstance(ParcelInstance inst) throws IllegalArgumentException {
    // Check: unique uuid
    if (parcelInstances.containsKey(inst.uuid())) {
      throw new IllegalArgumentException(
          "Parcel instance with uuid %s already exists".formatted(inst.uuid()));
    }

    // Check: bounding box no overlap
    for (var thatInst : parcelInstances.values()) {
      if (inst.boundingBox().intersects(thatInst.boundingBox())) {
        throw new IllegalArgumentException(
            "The new parcel instance intersects with existing parcel instance: %s <> %s"
                .formatted(inst.uuid(), thatInst.uuid()));
      }
    }

    setDirty();
    parcelInstances.put(inst.uuid(), inst);
  }

  public @Nullable ParcelInstance getParcelInstance(UUID uuid) {
    return parcelInstances.get(uuid);
  }

  public @Nullable ParcelInstance getParcelInstance(BlockPos pos) {
    for (var inst : parcelInstances.values()) {
      if (inst.boundingBox().isInside(pos)) {
        return inst;
      }
    }
    return null;
  }

  public static GitParcelSavedData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(TYPE);
  }

  public static void moveParcelInstance(ServerLevel fromLevel, ServerLevel toLevel, UUID uuid) {
    var from = fromLevel.getDataStorage().get(TYPE);
    if (from == null) {
      return;
    }
    var inst = from.getParcelInstances().remove(uuid);
    var to = toLevel.getDataStorage().computeIfAbsent(TYPE);
    to.getParcelInstances().put(uuid, inst);
  }
}
