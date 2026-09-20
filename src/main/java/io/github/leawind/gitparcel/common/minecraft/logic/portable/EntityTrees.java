package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Traversal over entity NBT trees, shared by the processors that recurse into the vanilla
 * {@code Passengers} structure and read each subtree's own type id.
 */
final class EntityTrees {
  private EntityTrees() {}

  /** Parses the entity type from the {@code id} key; null when absent, malformed, or not a string. */
  static @Nullable Identifier typeIdOf(CompoundTag data) {
    return Identifier.tryParse(NbtReads.getString(data, "id", ""));
  }

  /** Runs the visitor on every compound entry of the {@code Passengers} list, if any. */
  static void forEachPassenger(CompoundTag data, Consumer<CompoundTag> visitor) {
    var passengers = NbtReads.getList(data, "Passengers");
    if (passengers == null) {
      return;
    }
    for (Tag element : passengers) {
      if (element instanceof CompoundTag passenger) {
        visitor.accept(passenger);
      }
    }
  }
}
