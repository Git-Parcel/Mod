package io.github.leawind.gitparcel.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface AccessMinecraftServer {
  @Accessor("storageSource")
  LevelStorageSource.LevelStorageAccess getStorageSource();
}
