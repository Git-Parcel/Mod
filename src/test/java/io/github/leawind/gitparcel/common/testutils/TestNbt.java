package io.github.leawind.gitparcel.common.testutils;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;

/**
 * Version-neutral NBT reads in the Optional shape tests assert with.
 *
 * <p>26.x CompoundTag/ListTag getters return {@code Optional<T>} while 1.20.1 returns native
 * values, so test chains like {@code tag.getCompound("a").orElseThrow()} only compile on one side.
 * This facade mirrors the 26.x signatures on every node — absent or mistyped keys read as empty —
 * and is the only place in src/test that carries those conditionals. Production code uses the
 * fallback-shaped {@code NbtReads} seam instead.
 */
public final class TestNbt {
  private TestNbt() {}

  /** Reads an int, or empty when the key is absent or not numeric. */
  public static Optional<Integer> getInt(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getInt(key);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? Optional.of(tag.getInt(key)) : Optional.empty();
     *//*?}*/
  }

  /** Reads a long, or empty when the key is absent or not numeric. */
  public static Optional<Long> getLong(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getLong(key);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? Optional.of(tag.getLong(key)) : Optional.empty();
     *//*?}*/
  }

  /** Reads a short, or empty when the key is absent or not numeric. */
  public static Optional<Short> getShort(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getShort(key);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? Optional.of(tag.getShort(key)) : Optional.empty();
     *//*?}*/
  }

  /** Reads a byte, or empty when the key is absent or not numeric. */
  public static Optional<Byte> getByte(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getByte(key);
    /*?} else {*/
    /*return tag.get(key) instanceof NumericTag ? Optional.of(tag.getByte(key)) : Optional.empty();
     *//*?}*/
  }

  /** Reads a string, or empty when the key is absent or not a string. */
  public static Optional<String> getString(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getString(key);
    /*?} else {*/
    /*return tag.get(key) instanceof StringTag ? Optional.of(tag.getString(key)) : Optional.empty();
     *//*?}*/
  }

  /** Reads a compound, or empty when the key is absent or not a compound. */
  public static Optional<CompoundTag> getCompound(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getCompound(key);
    /*?} else {*/
    /*return tag.get(key) instanceof CompoundTag compound ? Optional.of(compound) : Optional.empty();
     *//*?}*/
  }

  /** Reads a list, or empty when the key is absent or not a list. */
  public static Optional<ListTag> getList(CompoundTag tag, String key) {
    /*? if >=26.1 {*/
    return tag.getList(key);
    /*?} else {*/
    /*return tag.get(key) instanceof ListTag list ? Optional.of(list) : Optional.empty();
     *//*?}*/
  }

  /** Reads a list element as a compound, or empty when out of bounds or not a compound. */
  public static Optional<CompoundTag> getCompound(ListTag list, int index) {
    /*? if >=26.1 {*/
    return list.getCompound(index);
    /*?} else {*/
    /*return index >= 0 && index < list.size() && list.get(index) instanceof CompoundTag compound
        ? Optional.of(compound)
        : Optional.empty();
     *//*?}*/
  }

  /** Reads a list element as a float, or empty when out of bounds or not numeric. */
  public static Optional<Float> getFloat(ListTag list, int index) {
    /*? if >=26.1 {*/
    return list.getFloat(index);
    /*?} else {*/
    /*return index >= 0 && index < list.size() && list.get(index) instanceof NumericTag number
        ? Optional.of(number.getAsFloat())
        : Optional.empty();
     *//*?}*/
  }

  /** Reads a list element as a double, or empty when out of bounds or not numeric. */
  public static Optional<Double> getDouble(ListTag list, int index) {
    /*? if >=26.1 {*/
    return list.getDouble(index);
    /*?} else {*/
    /*return index >= 0 && index < list.size() && list.get(index) instanceof NumericTag number
        ? Optional.of(number.getAsDouble())
        : Optional.empty();
     *//*?}*/
  }
}
