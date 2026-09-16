package io.github.leawind.gitparcel.common.impl.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.api.parcel.content.BlockSectionRegion;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class BlockSectionPartitionerTest extends AbstractMinecraftTest {
  private static final Vec3i SIZE_16X = new Vec3i(16, 16, 16);

  /**
   * @param size Size of the entire parcel
   * @param sections regions of every section
   */
  public static void assertParcelEqual(Vec3i size, Iterable<BlockSectionRegion> sections) {
    Set<BlockPos> blocks = new HashSet<>();

    for (var section : sections) {
      var bounds = section.bounds();
      for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
          for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
            BlockPos blockPos = new BlockPos(x, y, z);

            assertTrue(bounds.isInside(blockPos));
            assertFalse(blocks.contains(blockPos));
            blocks.add(blockPos);
          }
        }
      }
    }
    var volume = size.getX() * size.getY() * size.getZ();
    assertEquals(volume, blocks.size());
  }

  @Test
  void testCheck() {
    assertParcelEqual(
        SIZE_16X, //
        List.of(region(0, 0, 0, 16, 16, 16)));
    assertParcelEqual(
        SIZE_16X, //
        List.of(
            region(0, 0, 0, 16, 8, 16), //
            region(0, 8, 0, 16, 8, 16)));
  }

  @Test
  void testGetCoord() {
    assertEquals(Vec3i.ZERO, region(3, 4, 5, 3, 4, 5).gridCoordinate(16));
    assertEquals(
        new Vec3i(-1, -1, -1),
        region(-3, -4, -5, 3, 4, 5).gridCoordinate(16));
  }

  @Test
  void partitionsBlockSections() {
    {
      var result = BlockSectionPartitioner.partition(SIZE_16X, BlockPos.ZERO, 16);
      assertEquals(List.of(region(0, 0, 0, 16, 16, 16)), result);
    }
    {
      var result = BlockSectionPartitioner.partition(SIZE_16X, new BlockPos(4, 5, 6), 16);
      assertEquals(8, result.size());
      // Sections carry anchor-relative origins on the anchor-aligned grid.
      assertTrue(
          result.stream().anyMatch(section -> section.origin().equals(new BlockPos(0, 0, 0))));
      assertTrue(
          result.stream().anyMatch(section -> section.origin().equals(new BlockPos(-4, -5, -6))));
    }

    for (int i = 0; i < 1000; i++) {
      var size = random.nextVec3i(1, 50);
      assertParcelEqual(
          size, BlockSectionPartitioner.partition(size, random.nextVec3i(-100, 100), 16));
    }
  }

  @Test
  void gridLinesPassThroughTheAnchor() {
    // With the anchor itself on the section grid, the content bounds coincide with grid lines and
    // every section origin is a multiple of the section size.
    var result =
        BlockSectionPartitioner.partition(new Vec3i(32, 32, 32), new BlockPos(16, 0, 16), 16);
    assertEquals(8, result.size());
    for (var section : result) {
      assertEquals(0, Math.floorMod(section.origin().getX(), 16), section::toString);
      assertEquals(0, Math.floorMod(section.origin().getY(), 16), section::toString);
      assertEquals(0, Math.floorMod(section.origin().getZ(), 16), section::toString);
    }
  }

  @Test
  void gridIndicesAreStableAcrossBoundsChanges() {
    // Expanding the extent away from the anchor only adds sections; existing sections keep their
    // grid indices, so their files stay byte-identical after the bounds change.
    var small = BlockSectionPartitioner.partition(new Vec3i(4, 16, 16), new Vec3i(4, 0, 0), 16);
    var large = BlockSectionPartitioner.partition(new Vec3i(20, 16, 16), new Vec3i(4, 0, 0), 16);

    var smallIndices = small.stream().map(section -> section.gridCoordinate(16)).toList();
    var largeIndices = large.stream().map(section -> section.gridCoordinate(16)).toList();

    assertEquals(List.of(new Vec3i(-1, 0, 0)), smallIndices);
    assertTrue(largeIndices.containsAll(smallIndices));
    assertEquals(List.of(new Vec3i(-1, 0, 0), new Vec3i(0, 0, 0)), largeIndices);
  }

  @Test
  void testSubdivideParcel1D() {
    BiConsumer<List<Integer>, List<Integer>> test =
        (args, expected) -> {
          var result = BlockSectionPartitioner.partitionAxis(16, args.get(0), args.get(1));
          assertEquals(expected, result);
        };

    test.accept(List.of(0, 1), List.of(0, 1));
    test.accept(List.of(-4, 1), List.of(-4, 0, 1));
    test.accept(List.of(0, 37), List.of(0, 16, 32, 37));
    test.accept(List.of(0, 16), List.of(0, 16));
    test.accept(List.of(-16, 0), List.of(-16, 0));
    test.accept(List.of(16, 17), List.of(16, 17));
    test.accept(List.of(-1, 17), List.of(-1, 0, 16, 17));

    var random = new Random(12138);
    for (int i = 0; i < 10000; i++) {
      int start = random.nextInt(-100, 100);
      int end = start + random.nextInt(1, 1000);

      var result = BlockSectionPartitioner.partitionAxis(16, start, end);
      // assert ascending order
      for (int j = 0; j < result.size() - 1; j++) {
        assertTrue(result.get(j) <= result.get(j + 1));
      }
      int length = result.getLast() - result.getFirst();
      assertEquals(end - start, length);
    }
  }

  @Test
  void testFloorToGrid16() {
    assertEquals(-16, BlockSectionPartitioner.floorToGrid(16, -1));
    assertEquals(0, BlockSectionPartitioner.floorToGrid(16, 0));
    assertEquals(0, BlockSectionPartitioner.floorToGrid(16, 15));
    assertEquals(16, BlockSectionPartitioner.floorToGrid(16, 16));
    assertEquals(16, BlockSectionPartitioner.floorToGrid(16, 17));
    assertEquals(-16, BlockSectionPartitioner.floorToGrid(16, -16));
    assertEquals(-32, BlockSectionPartitioner.floorToGrid(16, -17));
  }

  @Test
  void testCeilToGrid16() {
    assertEquals(0, BlockSectionPartitioner.ceilToGrid(16, -1));
    assertEquals(16, BlockSectionPartitioner.ceilToGrid(16, 0));
    assertEquals(16, BlockSectionPartitioner.ceilToGrid(16, 15));
    assertEquals(32, BlockSectionPartitioner.ceilToGrid(16, 16));
    assertEquals(32, BlockSectionPartitioner.ceilToGrid(16, 17));
    assertEquals(0, BlockSectionPartitioner.ceilToGrid(16, -16));
    assertEquals(-16, BlockSectionPartitioner.ceilToGrid(16, -17));
  }

  private static BlockSectionRegion region(
      int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
    return new BlockSectionRegion(new BlockPos(x, y, z), new Vec3i(sizeX, sizeY, sizeZ));
  }
}
