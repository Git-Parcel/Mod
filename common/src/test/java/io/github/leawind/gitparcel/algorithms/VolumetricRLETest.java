package io.github.leawind.gitparcel.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.api.parcel.Parcel;
import io.github.leawind.gitparcel.parcelformats.parcella.utils.ZOrder3D;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;

public class VolumetricRLETest {

  public static final class TestedValues implements VolumetricRLE.ValueGetter {
    public final Vec3i size;
    private final int[][][] valuesArray;

    public TestedValues(Vec3i size, int valueVariance, RandomForMC random) {
      this.size = size;
      valuesArray = new int[size.getX()][size.getY()][size.getZ()];
      for (var pos : BlockPos.betweenClosed(1, 1, 1, size.getX(), size.getY(), size.getZ())) {
        valuesArray[pos.getX() - 1][pos.getY() - 1][pos.getZ() - 1] =
            random.nextInt(0, valueVariance);
      }
    }

    @Override
    public int get(int x, int y, int z) {
      try {
        return valuesArray[x][y][z];
      } catch (IndexOutOfBoundsException e) {
        throw e;
      }
    }
  }

  static void testAlgo(String name, VolumetricRLE.Encoder algo, int maxVariances) {
    var random = new RandomForMC(12138);

    System.out.println("Testing " + name);

    for (int variance = 1; variance <= maxVariances; variance *= 2) {
      double ratioSum = 0;
      double weightSum = 0;

      for (int i = 0; i < 32768; i += 17) {
        var size = ZOrder3D.indexToCoord(i).add(1, 1, 1);

        var values = new TestedValues(new Vec3i(size.x, size.y, size.z), variance, random);

        var runs = algo.encode(values.size.getX(), values.size.getY(), values.size.getZ(), values);
        assertRunsEqual(values.size, runs);

        int volume = values.size.getX() * values.size.getY() * values.size.getZ();
        var countRatio = (double) runs.size() / volume;
        ratioSum += countRatio * volume;
        weightSum += volume;
      }
      var ratio = ratioSum / weightSum;
      System.out.printf("Variance: %d, Average rate: %.2f\n", variance, ratio);
    }
  }

  @Test
  void testEncode() {
    for (var field : VolumetricRLE.class.getDeclaredFields()) {
      if (field.getType() == VolumetricRLE.class && field.canAccess(null)) {
        String name = field.getName();
        if (name.startsWith("_")) continue;
        try {
          testAlgo(name, (VolumetricRLE.Encoder) field.get(null), 8);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  public static <T extends Parcel> void assertRunsEqual(
      Vec3i size, Iterable<VolumetricRLE.Run> runs) {
    Set<BlockPos> blocks = new HashSet<>();

    for (var run : runs) {
      var bounds =
          new BoundingBox(run.minX(), run.minY(), run.minZ(), run.maxX(), run.maxY(), run.maxZ());

      for (int x = run.minX(); x < run.endX(); x++) {
        for (int y = run.minY(); y < run.endY(); y++) {
          for (int z = run.minZ(); z < run.endZ(); z++) {
            BlockPos minPos = new BlockPos(x, y, z);
            assertTrue(bounds.isInside(minPos));
            assertFalse(blocks.contains(minPos));
            blocks.add(minPos);
          }
        }
      }
    }
    var volume = size.getX() * size.getY() * size.getZ();
    assertEquals(volume, blocks.size());
  }
}
