package io.github.leawind.gitparcel.common.api.parcel.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Portable block-entity data at a block position relative to the parcel anchor. */
public record BlockEntityRecord(
    BlockPos pos, CompoundTag data, List<SemanticData> semanticData) {
  public BlockEntityRecord {
    pos = pos.immutable();
    data = data.copy();
    semanticData = List.copyOf(semanticData);
  }
}
