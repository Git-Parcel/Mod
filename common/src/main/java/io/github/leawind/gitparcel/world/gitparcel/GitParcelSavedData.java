package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

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

  public GitParcelSavedData() {
    this(new HashMap<>());
  }

  private GitParcelSavedData(Map<UUID, ParcelInstance> parcelInstance) {
    this.parcelInstances = parcelInstance;
  }

  private Map<UUID, ParcelInstance> getParcelInstances() {
    return parcelInstances;
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
