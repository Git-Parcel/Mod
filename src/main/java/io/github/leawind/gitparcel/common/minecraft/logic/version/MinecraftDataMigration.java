package io.github.leawind.gitparcel.common.minecraft.logic.version;

import com.mojang.serialization.Dynamic;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.ServerLevelAccessor;

/** Narrow bridge around Minecraft's type-specific DataFixer API. */
@VersionSensitive("Minecraft DataFixer type references and server accessor")
public final class MinecraftDataMigration {
  public enum Kind {
    BLOCK_ENTITY,
    ENTITY_TREE
  }

  private MinecraftDataMigration() {}

  public static CompoundTag update(
      ServerLevelAccessor level, Kind kind, CompoundTag data, int sourceDataVersion) {
    int current = MinecraftVersion.currentDataVersion();
    if (sourceDataVersion >= current) {
      return data.copy();
    }
    var server = level.getLevel().getServer();
    if (server == null) {
      throw new IllegalStateException("Cannot access Minecraft data fixer");
    }
    var reference =
        switch (kind) {
          case BLOCK_ENTITY -> References.BLOCK_ENTITY;
          case ENTITY_TREE -> References.ENTITY_TREE;
        };
    return (CompoundTag)
        server
            .getFixerUpper()
            .update(
                reference,
                new Dynamic<>(NbtOps.INSTANCE, data.copy()),
                sourceDataVersion,
                current)
            .getValue();
  }
}
