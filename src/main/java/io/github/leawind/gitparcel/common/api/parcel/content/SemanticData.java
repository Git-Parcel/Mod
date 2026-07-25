package io.github.leawind.gitparcel.common.api.parcel.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/** Versioned, processor-owned semantic data associated with a portable record. */
public record SemanticData(Identifier processor, int schemaVersion, CompoundTag payload) {
  public SemanticData {
    if (schemaVersion < 0) {
      throw new IllegalArgumentException("schemaVersion must be non-negative");
    }
    payload = payload.copy();
  }
}
