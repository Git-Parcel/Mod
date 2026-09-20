package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

/** Pins the unified read semantics: absent or mistyped keys fall back (or yield null). */
class NbtReadsTest extends AbstractMinecraftTest {
  @Test
  void readsIntsOnlyFromNumericTags() {
    var tag = new CompoundTag();
    tag.putInt("n", 7);
    tag.putString("s", "text");
    assertEquals(7, NbtReads.getInt(tag, "n", -1));
    assertEquals(-1, NbtReads.getInt(tag, "absent", -1));
    assertEquals(-1, NbtReads.getInt(tag, "s", -1));
    assertEquals(7, NbtReads.getIntOrNull(tag, "n"));
    assertNull(NbtReads.getIntOrNull(tag, "absent"));
    assertNull(NbtReads.getIntOrNull(tag, "s"));
  }

  @Test
  void readsStringsOnlyFromStringTags() {
    var tag = new CompoundTag();
    tag.putString("s", "value");
    tag.putInt("n", 1);
    assertEquals("value", NbtReads.getString(tag, "s", "fallback"));
    assertEquals("fallback", NbtReads.getString(tag, "absent", "fallback"));
    assertEquals("fallback", NbtReads.getString(tag, "n", "fallback"));
  }

  @Test
  void readsCompoundsListsAndArraysOnlyFromMatchingKinds() {
    var tag = new CompoundTag();
    var child = new CompoundTag();
    child.putInt("inner", 1);
    tag.put("compound", child);
    var list = new ListTag();
    list.add(StringTag.valueOf("element"));
    tag.put("list", list);
    tag.put("ids", new IntArrayTag(new int[] {1, 2, 3}));
    tag.putString("string", "text");

    assertEquals(child, NbtReads.getCompound(tag, "compound"));
    assertNull(NbtReads.getCompound(tag, "absent"));
    assertNull(NbtReads.getCompound(tag, "list"));
    assertEquals(list, NbtReads.getList(tag, "list"));
    assertNull(NbtReads.getList(tag, "absent"));
    assertNull(NbtReads.getList(tag, "compound"));
    assertArrayEquals(new int[] {1, 2, 3}, NbtReads.getIntArray(tag, "ids"));
    assertNull(NbtReads.getIntArray(tag, "absent"));
    assertNull(NbtReads.getIntArray(tag, "list"));
  }

  @Test
  void readsListElementsWithBoundsAndTypeChecks() {
    var list = new ListTag();
    list.add(DoubleTag.valueOf(1.5));
    list.add(DoubleTag.valueOf(-2.25));
    var mixed = new ListTag();
    mixed.add(StringTag.valueOf("text"));

    assertEquals(1.5, NbtReads.getDouble(list, 0, 9.0));
    assertEquals(-2.25, NbtReads.getDouble(list, 1, 9.0));
    assertEquals(9.0, NbtReads.getDouble(list, 2, 9.0));
    assertEquals(9.0, NbtReads.getDouble(mixed, 0, 9.0));
    assertEquals(1.5F, NbtReads.getFloat(list, 0, 9.0F));
    assertEquals(9.0F, NbtReads.getFloat(list, -1, 9.0F));
  }

  @Test
  void readsNumericTagValues() {
    assertEquals(42, NbtReads.intValue(IntTag.valueOf(42)));
    assertEquals(42L, NbtReads.longValue(IntTag.valueOf(42)));
    assertEquals(-1L, NbtReads.longValue(IntTag.valueOf(-1)));
  }

  @Test
  void readsThroughCodecs() {
    var tag = new CompoundTag();
    tag.put("block_pos", new IntArrayTag(new int[] {1, 2, 3}));
    tag.putString("text", "value");

    Optional<BlockPos> decoded = NbtReads.read(tag, "block_pos", BlockPos.CODEC);
    assertTrue(decoded.isPresent());
    assertEquals(new BlockPos(1, 2, 3), decoded.orElseThrow());
    assertTrue(NbtReads.read(tag, "absent", BlockPos.CODEC).isEmpty());
    assertTrue(NbtReads.read(tag, "text", BlockPos.CODEC).isEmpty());
  }
}
