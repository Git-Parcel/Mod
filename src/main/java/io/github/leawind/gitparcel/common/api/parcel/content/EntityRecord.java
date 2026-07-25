package io.github.leawind.gitparcel.common.api.parcel.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Portable root-entity data with positions relative to the parcel anchor. */
public record EntityRecord(
    Identifier type,
    Vec3 pos,
    BlockPos blockPos,
    CompoundTag data,
    List<SemanticData> semanticData) {
  public EntityRecord {
    blockPos = blockPos.immutable();
    data = data.copy();
    semanticData = List.copyOf(semanticData);
  }
}
