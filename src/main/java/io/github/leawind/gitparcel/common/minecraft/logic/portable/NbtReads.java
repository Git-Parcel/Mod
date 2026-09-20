package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Version-neutral NBT reads for shared processor code.
 *
 * <p>CompoundTag getters switched from native values (absent or mistyped keys read as zero or
 * empty) to Optionals partway through the supported version range, and the NumericTag reads were
 * renamed; every method below pins one semantics on every node: a read falls back (or yields
 * {@code null}) when the key is absent or carries the wrong kind of tag, so callers need no
 * per-node conditionals. Presence means "key present and of the expected kind"; {@link #getList}
 * accepts lists of any element kind, leaving element typing to the caller.
 */
public final class NbtReads {
  private NbtReads() {}

  /** Reads an int, or the fallback when the key is absent or not numeric. */
  public static int getInt(CompoundTag tag, String key, int fallback) {
    /*? if >=26.1 {*/
    return tag.getInt(key).orElse(fallback);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? tag.getInt(key) : fallback;
     *//*?}*/
  }

  /** Reads an int, or {@code null} when the key is absent or not numeric. */
  public static @Nullable Integer getIntOrNull(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getInt(key).orElse(null);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? tag.getInt(key) : null;
     *//*?}*/
  }

  /** Reads a string, or the fallback when the key is absent or not a string. */
  public static String getString(CompoundTag tag, String key, String fallback) {
    /*? if >=26.1 {*/
    return tag.getString(key).orElse(fallback);
    /*?} else {*/
    /*return tag.get(key) instanceof StringTag ? tag.getString(key) : fallback;
     *//*?}*/
  }

  /** Reads a compound, or {@code null} when the key is absent or not a compound. */
  public static @Nullable CompoundTag getCompound(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getCompound(key).orElse(null);
    /*?} else {*/
    /*return tag.get(key) instanceof CompoundTag compound ? compound : null;
     *//*?}*/
  }

  /** Reads a list of any element kind, or {@code null} when the key is absent or not a list. */
  public static @Nullable ListTag getList(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getList(key).orElse(null);
    /*?} else {*/
    /*return tag.get(key) instanceof ListTag list ? list : null;
     *//*?}*/
  }

  /** Reads an int array, or {@code null} when the key is absent or not an int array. */
  public static @Nullable int[] getIntArray(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getIntArray(key).orElse(null);
    /*?} else {*/
    /*return tag.get(key) instanceof IntArrayTag array ? array.getAsIntArray() : null;
     *//*?}*/
  }

  /** Reads a list element as a double, or the fallback when out of bounds or not numeric. */
  public static double getDouble(ListTag list, int index, double fallback) {
    /*? if >=26.1 {*/
    return list.getDouble(index).orElse(fallback);
    /*?} else {*/
    /*return index >= 0 && index < list.size() && list.get(index) instanceof NumericTag
        ? list.getDouble(index)
        : fallback;
     *//*?}*/
  }

  /** Reads a list element as a float, or the fallback when out of bounds or not numeric. */
  public static float getFloat(ListTag list, int index, float fallback) {
    /*? if >=26.1 {*/
    return list.getFloat(index).orElse(fallback);
    /*?} else {*/
    /*return index >= 0 && index < list.size() && list.get(index) instanceof NumericTag
        ? list.getFloat(index)
        : fallback;
     *//*?}*/
  }

  /** Reads the tag's numeric value as an int. */
  public static int intValue(NumericTag tag) {
    /*? if >=26.1 {*/
    return tag.intValue();
    /*?} else {*/
    /*return tag.getAsInt();
     *//*?}*/
  }

  /** Reads the tag's numeric value as a long. */
  public static long longValue(NumericTag tag) {
    /*? if >=26.1 {*/
    return tag.longValue();
    /*?} else {*/
    /*return tag.getAsLong();
     *//*?}*/
  }

  /** Decodes a value through a codec, or empty when the key is absent or fails to decode. */
  public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
    /*? if >=26.1 {*/
    return tag.read(key, codec);
    /*?} else {*/
    /*Tag child = tag.get(key);
    return child == null ? Optional.empty() : codec.parse(NbtOps.INSTANCE, child).result();
     *//*?}*/
  }
}
