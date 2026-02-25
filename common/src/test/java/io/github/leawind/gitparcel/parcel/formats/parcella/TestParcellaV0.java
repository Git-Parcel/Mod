package io.github.leawind.gitparcel.parcel.formats.parcella;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

public class TestParcellaV0 {

  static class TestSave {
    /**
     * @param total Total bounding box, including corners
     * @param subs Bounding boxes of each subparcel, including their corners
     */
    private static void assertEqual(BoundingBox total, Iterable<BoundingBox> subs) {
      Set<BlockPos> positions = new HashSet<>();

      for (BoundingBox sub : subs) {
        for (int x = sub.minX(); x <= sub.maxX(); x++) {
          for (int y = sub.minY(); y <= sub.maxY(); y++) {
            for (int z = sub.minZ(); z <= sub.maxZ(); z++) {
              BlockPos pos = new BlockPos(x, y, z);

              assert total.isInside(pos);
              assert !positions.contains(pos);
              positions.add(pos);
            }
          }
        }
      }

      assert positions.size() == total.getXSpan() * total.getYSpan() * total.getZSpan();
    }

    @Test
    void testSubdivideSubparcel() {
      {
        BlockPos from = new BlockPos(0, 0, 0);
        Vec3i size = new Vec3i(16, 16, 16);
        BlockPos to = from.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

        var result = ParcellaV0.Save.subdivideParcel(from, size, from);
        assert result.equals(List.of(new BoundingBox(0, 0, 0, 15, 15, 15)));
      }

      Random random = new Random(12138);
      for (int i = 0; i < 1000; i++) {
        BlockPos from =
            new BlockPos(
                random.nextInt(-1000, 1000),
                random.nextInt(-1000, 1000),
                random.nextInt(-1000, 1000));
        Vec3i size = new Vec3i(random.nextInt(1, 50), random.nextInt(1, 50), random.nextInt(1, 50));
        BlockPos to = from.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        Vec3i gridOrigin =
            new Vec3i(
                random.nextInt(-100, 100), //
                random.nextInt(-100, 100),
                random.nextInt(-100, 100));

        BoundingBox total = BoundingBox.fromCorners(from, to);
        assertEqual(total, ParcellaV0.Save.subdivideParcel(from, size, gridOrigin));
      }
    }

    @Test
    void testSubdivideParcel1D() {
      BiConsumer<List<Integer>, List<Integer>> test =
          (args, expected) -> {
            var result = ParcellaV0.Save.subdivideParcel1D(args.get(0), args.get(1), args.get(2));

            if (!result.equals(expected)) {
              System.out.println("Args: " + args);
              System.out.println("  Actual: " + result);
              System.out.println("  Expected: " + expected);

              throw new AssertionError("Axis division failed");
            }
          };

      test.accept(List.of(0, 1, 0), List.of(0, 1));
      test.accept(List.of(5, 1, 0), List.of(5, 6));

      test.accept(List.of(0, 37, 0), List.of(0, 16, 32, 37));
      test.accept(List.of(0, 16, 0), List.of(0, 16));
      test.accept(List.of(0, 16, 16), List.of(0, 16));

      test.accept(List.of(0, 17, 16), List.of(0, 16, 17));
      test.accept(List.of(0, 17, 17), List.of(0, 1, 17));
      test.accept(List.of(-2, 17, 17), List.of(-2, 1, 15));

      Random random = new Random(12138);
      for (int i = 0; i < 10000; i++) {
        int from = random.nextInt(-1000, 1000);
        int size = random.nextInt(1, 1000);
        int gridOrigin = random.nextInt(-100, 100);

        var result = ParcellaV0.Save.subdivideParcel1D(from, size, gridOrigin);
        int length = result.getLast() - result.getFirst();
        assert length == size;
      }
    }

    @Test
    void testFloorToGrid16() {
      assert ParcellaV0.Save.floorToGrid16(0, -1) == -16;
      assert ParcellaV0.Save.floorToGrid16(0, 0) == 0;
      assert ParcellaV0.Save.floorToGrid16(0, 15) == 0;
      assert ParcellaV0.Save.floorToGrid16(0, 16) == 16;
      assert ParcellaV0.Save.floorToGrid16(0, 17) == 16;

      assert ParcellaV0.Save.floorToGrid16(1, 1) == 1;
      assert ParcellaV0.Save.floorToGrid16(1, 0) == -15;

      assert ParcellaV0.Save.floorToGrid16(32, 1) == 0;
      assert ParcellaV0.Save.floorToGrid16(33, 0) == -15;

      assert ParcellaV0.Save.floorToGrid16(-32, 0) == 0;
      assert ParcellaV0.Save.floorToGrid16(-30, 17) == 2;
    }

    @Test
    void testCeilToGrid16() {
      assert ParcellaV0.Save.ceilToGrid16(0, -1) == 0;
      assert ParcellaV0.Save.ceilToGrid16(0, 0) == 16;
      assert ParcellaV0.Save.ceilToGrid16(0, 15) == 16;
      assert ParcellaV0.Save.ceilToGrid16(0, 16) == 32;
      assert ParcellaV0.Save.ceilToGrid16(0, 17) == 32;

      assert ParcellaV0.Save.ceilToGrid16(1, 1) == 17;
      assert ParcellaV0.Save.ceilToGrid16(1, 0) == 1;

      assert ParcellaV0.Save.ceilToGrid16(32, 1) == 16;
      assert ParcellaV0.Save.ceilToGrid16(33, 0) == 1;

      assert ParcellaV0.Save.ceilToGrid16(-32, 0) == 16;
      assert ParcellaV0.Save.ceilToGrid16(-30, 17) == 18;
    }

    @Test
    void testIndexToPath() {
      var cwd = Path.of(".");
      BiConsumer<Long, String> test =
          (index, path) -> {
            assert ParcellaV0.Save.indexToPath(cwd, index).equals(cwd.resolve(path));
          };
      test.accept(0x1234L, "34/12.txt");

      test.accept(0x00L, "00.txt");
      test.accept(0x01L, "01.txt");
      test.accept(0xFFL, "FF.txt");

      test.accept(0x0FL, "0F.txt");

      test.accept(0x240FL, "0F/24.txt");

      test.accept(0x01240FL, "0F/24/01.txt");
      test.accept(0x31240FL, "0F/24/31.txt");
    }
  }

  static class TestZOrder3D {

    /**
     * Tests the reversibility of unsigned coordinate-index conversion. Ensures that converting an
     * index to coordinates and back yields the original index.
     */
    @Test
    void testUnsignedIndexCoordinateReversibility() {
      for (long i = 0; i < 384 * 256 * 256; i++) {
        Vector3i coord = ParcellaV0.ZOrder3D.indexToCoord(i);
        long index = ParcellaV0.ZOrder3D.coordToIndex(coord);
        assert index == i;
      }
    }

    /**
     * Tests the uniqueness and correctness of unsigned coordinate-index mapping. Verifies that each
     * index maps to a unique coordinate within a 8x8x8 space.
     */
    @Test
    void testUnsignedIndexCoordinateUniquenessAndCorrectness() {
      Set<Vector3i> set = new HashSet<>();
      for (long i = 0; i < 8 * 8 * 8; i++) {
        Vector3i coord = ParcellaV0.ZOrder3D.indexToCoord(i);
        assert !set.contains(coord);
        set.add(coord);
      }
      assert (set.size() == 512);
      for (int x = 0; x < 8; x++) {
        for (int y = 0; y < 8; y++) {
          for (int z = 0; z < 8; z++) {
            Vector3i coord = new Vector3i(x, y, z);
            assert (set.contains(coord));
          }
        }
      }
    }

    /**
     * Tests the reversibility of signed coordinate-index conversion. Ensures that converting a
     * signed index to coordinates and back yields the original index.
     */
    @Test
    void testSignedIndexCoordinateReversibility() {
      for (long i = 0; i < 512; i++) {
        Vector3i coord = ParcellaV0.ZOrder3D.indexToCoordSigned(i);
        long index = ParcellaV0.ZOrder3D.coordToIndexSigned(coord);
        assert index == i;
      }
    }

    /**
     * Tests the uniqueness and correctness of signed coordinate-index mapping. Verifies that each
     * signed index maps to a unique coordinate within a (-4,-4,-4) to (3,3,3) space.
     */
    @Test
    void testSignedIndexCoordinateUniquenessAndCorrectness() {
      Set<Vector3i> set = new HashSet<>();
      for (long i = 0; i < 512; i++) {
        Vector3i coord = ParcellaV0.ZOrder3D.indexToCoordSigned(i);
        assert !set.contains(coord);
        set.add(coord);
      }
      assert (set.size() == 512);
      for (int x = -4; x < 4; x++) {
        for (int y = -4; y < 4; y++) {
          for (int z = -4; z < 4; z++) {
            Vector3i coord = new Vector3i(x, y, z);
            assert (set.contains(coord));
          }
        }
      }
    }
  }
}
