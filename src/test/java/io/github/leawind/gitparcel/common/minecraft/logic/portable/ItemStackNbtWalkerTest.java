package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.ArrayList;
import java.util.List;
import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class ItemStackNbtWalkerTest extends AbstractMinecraftTest {
  @Test
  void visitsEntityHandArmorAndSingleItems() throws ParcelException {
    var entity = new CompoundTag();
    var hands = new ListTag();
    hands.add(item("minecraft:filled_map"));
    hands.add(new CompoundTag());
    entity.put("HandItems", hands);
    var armor = new ListTag();
    armor.add(item("minecraft:diamond_helmet"));
    entity.put("ArmorItems", armor);
    entity.put("Item", item("minecraft:compass"));

    var visited = new ArrayList<String>();
    ItemStackNbtWalker.forEachEntityItem(entity, item -> visited.add(nameOf(item)));

    assertEquals(
        List.of(
            "minecraft:filled_map", "minecraft:diamond_helmet", "minecraft:compass"),
        visited);
  }

  @Test
  void visitsBlockEntityContainerAndBook() throws ParcelException {
    var blockEntity = new CompoundTag();
    var items = new ListTag();
    items.add(item("minecraft:stone"));
    items.add(item("minecraft:filled_map"));
    blockEntity.put("Items", items);
    blockEntity.put("Book", item("minecraft:written_book"));

    var visited = new ArrayList<String>();
    ItemStackNbtWalker.forEachBlockEntityItem(blockEntity, item -> visited.add(nameOf(item)));

    assertEquals(
        List.of("minecraft:stone", "minecraft:filled_map", "minecraft:written_book"), visited);
  }

  @Test
  void skipsAbsentContainers() throws ParcelException {
    var entity = new CompoundTag();
    var visited = new ArrayList<CompoundTag>();
    ItemStackNbtWalker.forEachEntityItem(entity, item -> visited.add(item));
    ItemStackNbtWalker.forEachBlockEntityItem(entity, item -> visited.add(item));
    assertEquals(List.of(), visited);
  }

  private static CompoundTag item(String id) {
    var item = new CompoundTag();
    item.putString("id", id);
    return item;
  }

  private static String nameOf(CompoundTag item) {
    return item.getString("id").orElse("");
  }
}
