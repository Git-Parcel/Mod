package io.github.leawind.gitparcel.parcel.formats.parcella;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class SubparcelTest {
  /**
   * @param parcel Total bounding box, including corners
   * @param subparcels Bounding boxes of each subparcel, including their corners
   */
  public static <T extends Parcel> void assertParcelEqual(Parcel parcel, Iterable<T> subparcels) {
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

    assertEquals(parcel.getVolume(), blocks.size());
  }

  @Test
  void testCheck() {
    assertParcelEqual(
        new Parcel(0, 0, 0, 16, 16, 16), //
        List.of(new Subparcel(0, 0, 0, 16, 16, 16)));
    assertParcelEqual(
        new Parcel(0, 0, 0, 16, 16, 16), //
        List.of(
            new Subparcel(0, 0, 0, 16, 8, 16), //
            new Subparcel(0, 8, 0, 16, 8, 16)));
  }

  @Test
  void testGetCoord() {
    assertEquals(Vec3i.ZERO, new Subparcel(3, 4, 5, 3, 4, 5).getCoord(Vec3i.ZERO));
    assertEquals(new Vec3i(-1, -1, -1), new Subparcel(-3, -4, -5, 3, 4, 5).getCoord(Vec3i.ZERO));
  }

  @Test
  void testSubdivideSubparcel() {
    {
      Parcel parcel = new Parcel(0, 0, 0, 16, 16, 16);
      var result = Subparcel.subdivideParcel(parcel, BlockPos.ZERO);
      assertEquals(List.of(new Subparcel(0, 0, 0, 16, 16, 16)), result);
    }
    {
      Parcel parcel = new Parcel(0, 0, 0, 16, 16, 16);
      var result = Subparcel.subdivideParcel(parcel, new BlockPos(4, 5, 6));
      assertEquals(8, result.size());
    }

    var random = new RandomForMC(12138);
    for (int i = 0; i < 1000; i++) {
      Parcel parcel = new Parcel(random.nextBlockPos(-1000, 1000), random.nextVec3i(1, 50));
      assertParcelEqual(parcel, Subparcel.subdivideParcel(parcel, random.nextVec3i(-100, 100)));
    }
  }

  @Test
  void testSubdivideParcel1D() {
    BiConsumer<List<Integer>, List<Integer>> test =
        (args, expected) -> {
          var result = Subparcel.subdivideParcel1D(args.get(0), args.get(1), args.get(2));
          assertEquals(expected, result);
        };

    test.accept(List.of(0, 1, 0), List.of(0, 1));
    test.accept(List.of(0, 1, -5), List.of(0, 1));
    test.accept(List.of(5, 1, 0), List.of(5, 6));
    test.accept(List.of(0, 37, 0), List.of(0, 16, 32, 37));
    test.accept(List.of(0, 16, 0), List.of(0, 16));
    test.accept(List.of(0, 16, 16), List.of(0, 16));
    test.accept(List.of(0, 17, 16), List.of(0, 16, 17));
    test.accept(List.of(0, 17, 17), List.of(0, 1, 17));
    test.accept(List.of(-2, 17, 17), List.of(-2, 1, 15));

    var random = new Random(12138);
    for (int i = 0; i < 10000; i++) {
      int from = random.nextInt(-1000, 1000);
      int size = random.nextInt(1, 1000);
      int anchor = random.nextInt(-100, 100);

      var result = Subparcel.subdivideParcel1D(from, size, anchor);
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
    assertEquals(-16, Subparcel.floorToGrid16(0, -1));
    assertEquals(0, Subparcel.floorToGrid16(0, 0));
    assertEquals(0, Subparcel.floorToGrid16(0, 15));
    assertEquals(16, Subparcel.floorToGrid16(0, 16));
    assertEquals(16, Subparcel.floorToGrid16(0, 17));

    assertEquals(1, Subparcel.floorToGrid16(1, 1));
    assertEquals(-15, Subparcel.floorToGrid16(1, 0));

    assertEquals(0, Subparcel.floorToGrid16(32, 1));
    assertEquals(-15, Subparcel.floorToGrid16(33, 0));

    assertEquals(0, Subparcel.floorToGrid16(-32, 0));
    assertEquals(2, Subparcel.floorToGrid16(-30, 17));
  }

  @Test
  void testCeilToGrid16() {
    assertEquals(0, Subparcel.ceilToGrid16(0, -1));
    assertEquals(16, Subparcel.ceilToGrid16(0, 0));
    assertEquals(16, Subparcel.ceilToGrid16(0, 15));
    assertEquals(32, Subparcel.ceilToGrid16(0, 16));
    assertEquals(32, Subparcel.ceilToGrid16(0, 17));

    assertEquals(17, Subparcel.ceilToGrid16(1, 1));
    assertEquals(1, Subparcel.ceilToGrid16(1, 0));

    assertEquals(16, Subparcel.ceilToGrid16(32, 1));
    assertEquals(1, Subparcel.ceilToGrid16(33, 0));

    assertEquals(16, Subparcel.ceilToGrid16(-32, 0));
    assertEquals(18, Subparcel.ceilToGrid16(-30, 17));
  }
}
