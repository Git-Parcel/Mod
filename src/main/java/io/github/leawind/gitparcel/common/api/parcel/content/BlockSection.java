package io.github.leawind.gitparcel.common.api.parcel.content;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A bounded block section in parcel space.
 *
 * <p>States use x-major, then y, then z ordering.
 */
public record BlockSection(
    BlockPos origin,
    Vec3i size,
    List<BlockState> states,
    List<BlockEntityRecord> blockEntities) {
  public BlockSection {
    origin = origin.immutable();
    size = new Vec3i(size.getX(), size.getY(), size.getZ());
    states = List.copyOf(states);
    blockEntities = List.copyOf(blockEntities);
    if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
      throw new IllegalArgumentException("Block section dimensions must be positive: " + size);
    }
    int expected = Math.multiplyExact(Math.multiplyExact(size.getX(), size.getY()), size.getZ());
    if (states.size() != expected) {
      throw new IllegalArgumentException(
          "Expected " + expected + " block states, got " + states.size());
    }
  }

  public int index(int x, int y, int z) {
    return (x * size.getY() + y) * size.getZ() + z;
  }

  public BlockState state(int x, int y, int z) {
    return states.get(index(x, y, z));
  }
}
