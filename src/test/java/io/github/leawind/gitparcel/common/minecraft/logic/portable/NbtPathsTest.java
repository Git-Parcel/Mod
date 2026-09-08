package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class NbtPathsTest extends AbstractMinecraftTest {
  @Test
  void parsesCompoundListAndIndexSegments() {
    assertEquals(List.of("flower_pos"), NbtPaths.parse("flower_pos"));
    assertEquals(
        List.of("Items", -1, "tag", "waypoint"), NbtPaths.parse("Items[].tag.waypoint"));
    assertEquals(List.of("Runs", 2), NbtPaths.parse("Runs[2]"));
  }

  @Test
  void rejectsMalformedPaths() {
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse(""));
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse("a..b"));
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse(".a"));
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse("a."));
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse("[0]"));
    assertThrows(IllegalArgumentException.class, () -> NbtPaths.parse("a[x]"));
  }

  @Test
  void visitsAndReplacesCompoundEntries() {
    var root = new CompoundTag();
    root.putInt("value", 1);

    List<Integer> seen = new ArrayList<>();
    NbtPaths.forEach(
        root,
        NbtPaths.parse("value"),
        slot -> {
          seen.add((int) slot.get().getId());
          slot.set(IntTag.valueOf(7));
        });

    assertEquals(1, seen.size());
    assertEquals(7, root.getInt("value").orElseThrow());
  }

  @Test
  void visitsEveryListElementForWildcard() {
    var root = new CompoundTag();
    var items = new ListTag();
    for (int i = 0; i < 3; i++) {
      var item = new CompoundTag();
      item.putInt("count", i);
      items.add(item);
    }
    root.put("Items", items);

    NbtPaths.forEach(
        root,
        NbtPaths.parse("Items[].count"),
        slot ->
            slot.set(
                IntTag.valueOf(((net.minecraft.nbt.NumericTag) slot.get()).intValue() + 10)));

    for (int i = 0; i < 3; i++) {
      assertEquals(i + 10, items.getCompound(i).orElseThrow().getInt("count").orElseThrow());
    }
  }

  @Test
  void skipsMissingPathsAndUnexpectedTypes() {
    var root = new CompoundTag();
    var visits = new ArrayList<>();
    NbtPaths.forEach(root, NbtPaths.parse("missing"), slot -> visits.add(slot));
    NbtPaths.forEach(root, NbtPaths.parse("Items[]"), slot -> visits.add(slot));

    root.put("Items", IntTag.valueOf(3));
    NbtPaths.forEach(root, NbtPaths.parse("Items[]"), slot -> visits.add(slot));
    NbtPaths.forEach(root, NbtPaths.parse("Items[5]"), slot -> visits.add(slot));

    assertEquals(List.of(), visits);
  }
}
