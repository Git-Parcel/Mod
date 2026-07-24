package io.github.leawind.gitparcel.common.minecraft.logic.world;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
/*? if >=1.20.5 && <1.21.11 {*/
/*import net.minecraft.core.HolderLookup;
 *//*?}*/
/*? if <1.21.11 {*/
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
 *//*?}*/
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A codec-backed saved-data model across Minecraft's old and new persistence APIs.
 *
 * <p>Since 1.21.11 Minecraft owns codec serialization through {@code SavedDataType}. Older
 * versions require each {@link SavedData} subclass to implement NBT serialization itself. This
 * base class keeps that compatibility detail out of the actual world-state models.
 */
@VersionSensitive("Minecraft SavedData serialization API")
abstract class CodecSavedData<T extends CodecSavedData<T>> extends SavedData {
  private final Codec<T> codec;

  protected CodecSavedData(Codec<T> codec) {
    this.codec = codec;
  }

  /*? if >=1.20.5 && <1.21.11 {*/
  /*@Override
  public final CompoundTag save(CompoundTag target, HolderLookup.Provider registries) {
    return (CompoundTag)
        codec
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), self())
            .getOrThrow();
  }

  static <T> T decode(
      Codec<T> codec, CompoundTag tag, HolderLookup.Provider registries) {
    return codec
        .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
        .getOrThrow();
  }
  *//*?} else if <1.20.5 {*/
  /*@Override
  public final CompoundTag save(CompoundTag target) {
    return (CompoundTag)
        codec
            .encodeStart(NbtOps.INSTANCE, self())
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

  @SuppressWarnings("unchecked")
  private T self() {
    return (T) this;
  }
}
