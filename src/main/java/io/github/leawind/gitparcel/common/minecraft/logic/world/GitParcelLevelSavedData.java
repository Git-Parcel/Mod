package io.github.leawind.gitparcel.common.minecraft.logic.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class GitParcelLevelSavedData extends CodecSavedData<GitParcelLevelSavedData> {
  public static final Codec<GitParcelLevelSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Parcels.CODEC.fieldOf("parcels").forGetter(GitParcelLevelSavedData::parcels))
                  .apply(inst, GitParcelLevelSavedData::new));

  private final Parcels parcels;

  GitParcelLevelSavedData() {
    this(new Parcels());
  }

  private GitParcelLevelSavedData(Parcels parcels) {
    super(CODEC);
    this.parcels = parcels;
  }

  public Parcels parcels() {
    return parcels;
  }

  public void clearParcels() {
    parcels.clear();
    setDirty();
  }

  public void addParcel(Parcel parcel) {
    parcels.put(parcel);
    setDirty();
  }

  public @Nullable Parcel removeParcel(UUID uuid) {
    var result = parcels.remove(uuid);
    if (result != null) {
      setDirty();
    }
    return result;
  }

  public static GitParcelLevelSavedData get(ServerLevel level) {
    return GitParcelSavedDataAccess.level(level);
  }
}
