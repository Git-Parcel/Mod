package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.exceptions.ParcelException;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

/**
 * Visits item-stack compounds stored inside entity and block-entity NBT.
 *
 * <p>Covers the vanilla container shapes: entity {@code HandItems}/{@code ArmorItems} lists and
 * the {@code Item} compound of item frames, block-entity {@code Items} lists and the lectern {@code
 * Book} compound. Custom containers can be reached with {@link NbtPaths} directly.
 */
public final class ItemStackNbtWalker {
  /** Visits one item-stack compound; may fail with a parcel error. */
  @FunctionalInterface
  public interface ItemVisitor {
    void accept(CompoundTag item) throws ParcelException;
  }

  private static final List<String> ENTITY_LIST_KEYS = List.of("HandItems", "ArmorItems");
  private static final List<String> ENTITY_COMPOUND_KEYS = List.of("Item");
  private static final List<String> BLOCK_ENTITY_LIST_KEYS = List.of("Items");
  private static final List<String> BLOCK_ENTITY_COMPOUND_KEYS = List.of("Book");

  private ItemStackNbtWalker() {}

  public static void forEachEntityItem(CompoundTag entityData, ItemVisitor item)
      throws ParcelException {
    visitLists(entityData, ENTITY_LIST_KEYS, item);
    visitCompounds(entityData, ENTITY_COMPOUND_KEYS, item);
  }

  public static void forEachBlockEntityItem(CompoundTag blockEntityData, ItemVisitor item)
      throws ParcelException {
    visitLists(blockEntityData, BLOCK_ENTITY_LIST_KEYS, item);
    visitCompounds(blockEntityData, BLOCK_ENTITY_COMPOUND_KEYS, item);
  }

  private static void visitLists(CompoundTag data, List<String> keys, ItemVisitor item)
      throws ParcelException {
    for (String key : keys) {
      var list = data.getList(key);
      if (list.isEmpty()) {
        continue;
      }
      for (var element : list.orElseThrow()) {
        if (element instanceof CompoundTag compound && !compound.isEmpty()) {
          item.accept(compound);
        }
      }
    }
  }

  private static void visitCompounds(CompoundTag data, List<String> keys, ItemVisitor item)
      throws ParcelException {
    for (String key : keys) {
      var compound = data.getCompound(key);
      if (compound.isPresent()) {
        item.accept(compound.orElseThrow());
      }
    }
  }
}
