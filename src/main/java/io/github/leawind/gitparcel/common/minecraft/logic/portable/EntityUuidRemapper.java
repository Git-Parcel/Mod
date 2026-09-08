package io.github.leawind.gitparcel.common.minecraft.logic.portable;

import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefField;
import io.github.leawind.gitparcel.common.api.extension.field.ParcelEntityRefFieldRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Rewrites entity-UUID references when a restore batch receives fresh UUIDs.
 *
 * <p>References pointing outside the batch (players, entities in other parcels) are deliberately
 * left untouched.
 */
final class EntityUuidRemapper {
  private EntityUuidRemapper() {}

  /** Assigns a fresh UUID to every original; duplicates collapse to one entry. */
  static Map<UUID, UUID> assignFreshIds(List<UUID> originalIds) {
    var remap = new HashMap<UUID, UUID>();
    for (UUID original : originalIds) {
      remap.computeIfAbsent(original, ignored -> UUID.randomUUID());
    }
    return remap;
  }

  /**
   * Rewrites declared reference fields inside the tag tree, including the Passengers subtree with
   * the same scoping as the top-level entity.
   */
  static void rewriteReferences(
      CompoundTag data, @Nullable Identifier entityType, Map<UUID, UUID> remap) {
    if (remap.isEmpty()) {
      return;
    }
    rewriteTree(data, entityType, remap);
    data.getList("Passengers")
        .ifPresent(
            passengers ->
                passengers
                    .compoundStream()
                    .forEach(
                        passenger ->
                            rewriteTree(
                                passenger,
                                passenger.getString("id").map(Identifier::parse).orElse(null),
                                remap)));
  }

  private static void rewriteTree(
      CompoundTag data, @Nullable Identifier entityType, Map<UUID, UUID> remap) {
    for (ParcelEntityRefField field : ParcelEntityRefFieldRegistry.get().fields()) {
      if (!field.appliesTo(entityType)) {
        continue;
      }
      NbtPaths.forEach(
          data,
          NbtPaths.parse(field.path()),
          slot -> rewriteSlot(slot, remap));
    }
  }

  private static void rewriteSlot(NbtPaths.Slot slot, Map<UUID, UUID> remap) {
    Tag tag = slot.get();
    if (tag == null) {
      return;
    }
    Optional<UUID> original = UUIDUtil.CODEC.parse(NbtOps.INSTANCE, tag).result();
    if (original.isEmpty()) {
      return;
    }
    UUID replacement = remap.get(original.orElseThrow());
    if (replacement != null) {
      slot.set(UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, replacement).getOrThrow());
    }
  }
}
