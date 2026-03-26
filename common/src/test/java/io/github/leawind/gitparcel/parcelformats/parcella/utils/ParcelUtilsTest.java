package io.github.leawind.gitparcel.parcelformats.parcella.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.testutils.AbstractGitParcelTest;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class ParcelUtilsTest extends AbstractGitParcelTest {
  private static final Vec3i SIZE_16X = new Vec3i(16, 16, 16);

  /**
   * @param size Size of the entire parcel
   * @param subparcels Bounding boxes of each subparcel, including their corners
   */
  public static void assertParcelEqual(Vec3i size, Iterable<Subparcel> subparcels) {
    Set<BlockPos> blocks = new HashSet<>();

    for (var subparcel : subparcels) {
      for (int x = subparcel.originX; x <= subparcel.getMaxX(); x++) {
        for (int y = subparcel.originY; y <= subparcel.getMaxY(); y++) {
          for (int z = subparcel.originZ; z <= subparcel.getMaxZ(); z++) {
            BlockPos blockPos = new BlockPos(x, y, z);

            assertTrue(subparcel.getBoundingBox().isInside(blockPos));
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
        List.of(new Subparcel(0, 0, 0, 16, 16, 16)));
    assertParcelEqual(
        SIZE_16X, //
        List.of(
            new Subparcel(0, 0, 0, 16, 8, 16), //
            new Subparcel(0, 8, 0, 16, 8, 16)));
  }

  @Test
  void testGetCoord() {
    assertEquals(Vec3i.ZERO, new Subparcel(3, 4, 5, 3, 4, 5).getCoord(16, Vec3i.ZERO));
    assertEquals(
        new Vec3i(-1, -1, -1), new Subparcel(-3, -4, -5, 3, 4, 5).getCoord(16, Vec3i.ZERO));
  }

  @Test
  void testSubdivideSubparcel() {
    {
      var result = ParcelUtils.subdivideParcel(SIZE_16X, BlockPos.ZERO, 16);
      assertEquals(List.of(new Subparcel(0, 0, 0, 16, 16, 16)), result);
    }
    {
      var result = ParcelUtils.subdivideParcel(SIZE_16X, new BlockPos(4, 5, 6), 16);
      assertEquals(8, result.size());
    }

    for (int i = 0; i < 1000; i++) {
      var size = random.nextVec3i(1, 50);
      assertParcelEqual(size, ParcelUtils.subdivideParcel(size, random.nextVec3i(-100, 100), 16));
    }
  }

  @Test
  void testSubdivideParcel1D() {
    BiConsumer<List<Integer>, List<Integer>> test =
        (args, expected) -> {
          var result = ParcelUtils.subdivideParcel1D(16, args.get(0), args.get(1));
          assertEquals(expected, result);
        };

    test.accept(List.of(1, 0), List.of(0, 1));
    test.accept(List.of(1, -5), List.of(0, 1));
    test.accept(List.of(37, 0), List.of(0, 16, 32, 37));
    test.accept(List.of(16, 0), List.of(0, 16));
    test.accept(List.of(16, 16), List.of(0, 16));
    test.accept(List.of(17, 16), List.of(0, 16, 17));
    test.accept(List.of(17, 17), List.of(0, 1, 17));

    var random = new Random(12138);
    for (int i = 0; i < 10000; i++) {
      int size = random.nextInt(1, 1000);
      int anchor = random.nextInt(-100, 100);

      var result = ParcelUtils.subdivideParcel1D(16, size, anchor);
      // assert ascending order
      for (int j = 0; j < result.size() - 1; j++) {
        assertTrue(result.get(j) <= result.get(j + 1));
      }
      int length = result.getLast() - result.getFirst();
      assertEquals(size, length);
    }
  }

  @Test
  void testFloorToGrid16() {
    assertEquals(-16, ParcelUtils.floorToGrid(16, 0, -1));
    assertEquals(0, ParcelUtils.floorToGrid(16, 0, 0));
    assertEquals(0, ParcelUtils.floorToGrid(16, 0, 15));
    assertEquals(16, ParcelUtils.floorToGrid(16, 0, 16));
    assertEquals(16, ParcelUtils.floorToGrid(16, 0, 17));

    assertEquals(1, ParcelUtils.floorToGrid(16, 1, 1));
    assertEquals(-15, ParcelUtils.floorToGrid(16, 1, 0));

    assertEquals(0, ParcelUtils.floorToGrid(16, 32, 1));
    assertEquals(-15, ParcelUtils.floorToGrid(16, 33, 0));

    assertEquals(0, ParcelUtils.floorToGrid(16, -32, 0));
    assertEquals(2, ParcelUtils.floorToGrid(16, -30, 17));
  }

  @Test
  void testCeilToGrid16() {
    assertEquals(0, ParcelUtils.ceilToGrid(16, 0, -1));
    assertEquals(16, ParcelUtils.ceilToGrid(16, 0, 0));
    assertEquals(16, ParcelUtils.ceilToGrid(16, 0, 15));
    assertEquals(32, ParcelUtils.ceilToGrid(16, 0, 16));
    assertEquals(32, ParcelUtils.ceilToGrid(16, 0, 17));

    assertEquals(17, ParcelUtils.ceilToGrid(16, 1, 1));
    assertEquals(1, ParcelUtils.ceilToGrid(16, 1, 0));

    assertEquals(16, ParcelUtils.ceilToGrid(16, 32, 1));
    assertEquals(1, ParcelUtils.ceilToGrid(16, 33, 0));

    assertEquals(16, ParcelUtils.ceilToGrid(16, -32, 0));
    assertEquals(18, ParcelUtils.ceilToGrid(16, -30, 17));
  }
}
