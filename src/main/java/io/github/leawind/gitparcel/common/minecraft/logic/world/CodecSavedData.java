package io.github.leawind.gitparcel.common.minecraft.logic.world;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
/*? if <26.1 {*/
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
 *//*?}*/

/**
 * Codec helpers for Minecraft's SavedData serialization, whose carrier changed across versions.
 *
 * <p>Since 1.21.11 Minecraft owns codec serialization through {@code SavedDataType}, so the
 * helpers are only needed on older versions.
 */
@VersionSensitive("Minecraft SavedData serialization API")
final class CodecSavedData {
  private CodecSavedData() {}

  /*? if <26.1 {*/
  /*static <T> CompoundTag encode(Codec<T> codec, T value) {
    return (CompoundTag)
        codec
            .encodeStart(NbtOps.INSTANCE, value)
            .result()
            .orElseThrow(() -> new IllegalStateException("Failed to encode saved data"));
  }

  static <T> T decode(Codec<T> codec, CompoundTag tag) {
    return codec
        .parse(NbtOps.INSTANCE, tag)
        .result()
        .orElseThrow(() -> new IllegalStateException("Failed to decode saved data"));
  }
  *//*?}*/
}
