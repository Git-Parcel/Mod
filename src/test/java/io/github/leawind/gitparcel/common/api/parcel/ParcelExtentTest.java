package io.github.leawind.gitparcel.common.api.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class ParcelExtentTest {
  private final ParcelExtent extent = new ParcelExtent(new Vec3i(8, 8, 8), new Vec3i(2, 1, 0));

  @Test
  void spansAnchorRelativeHalfOpenBounds() {
    assertEquals(new Vec3i(-2, -1, 0), extent.minCorner());
    assertEquals(new Vec3i(6, 7, 8), extent.maxCorner());
  }

  @Test
  void includesTheLowerBoundAndExcludesTheUpperBound() {
    assertTrue(extent.contains(new Vec3(-2, -1, 0)));
    assertTrue(extent.contains(new Vec3(5.75, 6.5, 7.5)));
    assertFalse(extent.contains(new Vec3(6, 0, 0)), "x at the exclusive upper bound is outside");
    assertFalse(extent.contains(new Vec3(0, 7, 0)), "y at the exclusive upper bound is outside");
    assertFalse(extent.contains(new Vec3(0, 0, 8)), "z at the exclusive upper bound is outside");
    assertFalse(extent.contains(new Vec3(-2.125, 0, 0)));
  }

  @Test
  void acceptsBlockPositions() {
    assertTrue(extent.contains(new BlockPos(-2, -1, 0)));
    assertFalse(extent.contains(new BlockPos(6, 0, 0)));
  }

  @Test
  void rejectsNonPositiveSizes() {
    assertThrows(IllegalArgumentException.class, () -> new ParcelExtent(Vec3i.ZERO, Vec3i.ZERO));
  }
}
