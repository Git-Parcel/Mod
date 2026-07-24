package io.github.leawind.gitparcel.common.minecraft.logic.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

final class GitParcelLevelSavedData extends SavedData {
  private static final Codec<GitParcelLevelSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Parcels.CODEC.fieldOf("parcels").forGetter(GitParcelLevelSavedData::parcels))
                  .apply(inst, GitParcelLevelSavedData::new));

  private static final SavedDataType<GitParcelLevelSavedData> TYPE =
      new SavedDataType<>(
          GitParcelUtils.identifier("level"), GitParcelLevelSavedData::new, CODEC, null);

  private final Parcels parcels;

  private GitParcelLevelSavedData() {
    this(new Parcels());
  }

  private GitParcelLevelSavedData(Parcels parcels) {
    this.parcels = parcels;
  }

  Parcels parcels() {
    return parcels;
  }

  void clearParcels() {
    parcels.clear();
    setDirty();
  }

  void addParcel(Parcel parcel) {
    parcels.put(parcel);
    setDirty();
  }

  @Nullable Parcel removeParcel(UUID uuid) {
    var result = parcels.remove(uuid);
    if (result != null) {
      setDirty();
    }
    return result;
  }

  static GitParcelLevelSavedData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(TYPE);
  }
}
