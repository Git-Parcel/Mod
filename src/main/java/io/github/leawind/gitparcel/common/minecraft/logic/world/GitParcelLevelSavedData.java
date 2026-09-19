package io.github.leawind.gitparcel.common.minecraft.logic.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
/*? if <26.1 {*/
/*import net.minecraft.nbt.CompoundTag;
 *//*?}*/
import net.minecraft.world.level.saveddata.SavedData;
import org.jspecify.annotations.Nullable;

public final class GitParcelLevelSavedData extends SavedData {
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
    this.parcels = parcels;
  }

  /*? if <26.1 {*/
  /*@Override
  public CompoundTag save(CompoundTag tag) {
    return CodecSavedData.encode(CODEC, this);
  }
  *//*?}*/

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
