package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Visits item-stack compounds stored inside entity and block-entity NBT.
 *
 * <p>Covers the vanilla container shapes: entity {@code HandItems}/{@code ArmorItems}/{@code
 * Inventory} lists, the {@code Item} compound of item entities and item frames, block-entity
 * {@code Items} lists, the lectern {@code Book} and jukebox {@code RecordItem} compounds, the
 * brushable-block {@code item} compound, and the trade slots of merchant {@code Offers}
 * ({@code Recipes[].buy/buyB/sell}). Custom containers can be reached with {@link NbtPaths}
 * directly.
 */
public final class ItemStackNbtWalker {
  /** Visits one item-stack compound; may fail with a parcel error. */
  @FunctionalInterface
  public interface ItemVisitor {
    void accept(CompoundTag item) throws ParcelException;
  }

  private static final List<String> ENTITY_LIST_KEYS = List.of("HandItems", "ArmorItems", "Inventory");
  private static final List<String> ENTITY_COMPOUND_KEYS = List.of("Item");
  private static final List<String> ENTITY_ITEM_PATHS =
      List.of("Offers.Recipes[].buy", "Offers.Recipes[].buyB", "Offers.Recipes[].sell");
  private static final List<String> BLOCK_ENTITY_LIST_KEYS = List.of("Items");
  private static final List<String> BLOCK_ENTITY_COMPOUND_KEYS = List.of("Book", "RecordItem", "item");

  private ItemStackNbtWalker() {}

  public static void forEachEntityItem(CompoundTag entityData, ItemVisitor item)
      throws ParcelException {
    visitLists(entityData, ENTITY_LIST_KEYS, item);
    visitCompounds(entityData, ENTITY_COMPOUND_KEYS, item);
    visitPaths(entityData, ENTITY_ITEM_PATHS, item);
  }

  public static void forEachBlockEntityItem(CompoundTag blockEntityData, ItemVisitor item)
      throws ParcelException {
    visitLists(blockEntityData, BLOCK_ENTITY_LIST_KEYS, item);
    visitCompounds(blockEntityData, BLOCK_ENTITY_COMPOUND_KEYS, item);
  }

  private static void visitLists(CompoundTag data, List<String> keys, ItemVisitor item)
      throws ParcelException {
    for (String key : keys) {
      var list = NbtReads.getList(data, key);
      if (list == null) {
        continue;
      }
      for (Tag element : list) {
        if (element instanceof CompoundTag compound && !compound.isEmpty()) {
          item.accept(compound);
        }
      }
    }
  }

  private static void visitCompounds(CompoundTag data, List<String> keys, ItemVisitor item)
      throws ParcelException {
    for (String key : keys) {
      var compound = NbtReads.getCompound(data, key);
      if (compound != null && !compound.isEmpty()) {
        item.accept(compound);
      }
    }
  }

  private static void visitPaths(CompoundTag data, List<String> paths, ItemVisitor item)
      throws ParcelException {
    for (String path : paths) {
      // NbtPaths consumers cannot throw, so matches are collected before visiting.
      List<CompoundTag> found = new ArrayList<>();
      NbtPaths.forEach(
          data,
          NbtPaths.parse(path),
          slot -> {
            if (slot.get() instanceof CompoundTag compound && !compound.isEmpty()) {
              found.add(compound);
            }
          });
      for (CompoundTag compound : found) {
        item.accept(compound);
      }
    }
  }
}
