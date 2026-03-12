package io.github.leawind.gitparcel.world.gitparcel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.permission.GitParcelPermission;
import io.github.leawind.gitparcel.permission.PermissionSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class GitParcelWorldSavedData extends SavedData {
  public static final Codec<GitParcelWorldSavedData> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      GitParcelPermission.SETTINGS_MAP_CODEC
                          .fieldOf("permissions")
                          .forGetter(GitParcelWorldSavedData::getPermissions))
                  .apply(inst, GitParcelWorldSavedData::new));

  public static final SavedDataType<GitParcelWorldSavedData> TYPE =
      new SavedDataType<>(
          "gitparcel_world", GitParcelWorldSavedData::new, CODEC, DataFixTypes.LEVEL);

  private final PermissionSettings permissions;

  public PermissionSettings getPermissions() {
    return permissions;
  }

  private GitParcelWorldSavedData() {
    this(new PermissionSettings(GitParcelPermission.REGISTRY));
  }

  private GitParcelWorldSavedData(PermissionSettings permissions) {
    this.permissions = permissions;
  }

  public static GitParcelWorldSavedData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(TYPE);
  }
}
