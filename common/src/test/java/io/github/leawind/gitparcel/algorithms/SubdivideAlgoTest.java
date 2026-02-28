package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.SubparcelTest;
import io.github.leawind.gitparcel.parcel.formats.parcella.ZOrder3D;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class SubdivideAlgoTest {

  public static class ParcelWithValue extends Parcel implements Parcel.WithValue {
    private final int value;

    public ParcelWithValue(
        int value, int originX, int originY, int originZ, int sizeX, int sizeY, int sizeZ) {
      super(originX, originY, originZ, sizeX, sizeY, sizeZ);
      this.value = value;
    }

    @Override
    public int getValue() {
      return value;
    }
  }

  public record TestCase(Parcel parcel, int[][][] valuesArray) implements SubdivideAlgo.Values {
    @Override
    public int get(int x, int y, int z) {
      return valuesArray[x - parcel.originX][y - parcel.originY][z - parcel.originZ];
    }

    static TestCase create(RandomForMC random, Parcel parcel, int valueVariance) {
      int[][][] values = new int[parcel.sizeX][parcel.sizeY][parcel.sizeZ];
      for (var pos : BlockPos.betweenClosed(1, 1, 1, parcel.sizeX, parcel.sizeY, parcel.sizeZ)) {
        values[pos.getX() - 1][pos.getY() - 1][pos.getZ() - 1] = random.nextInt(0, valueVariance);
      }
      return new TestCase(parcel, values);
    }
  }

  static void testAlgo(SubdivideAlgo algo, int maxVariances) {
    var random = new RandomForMC(12138);

    System.out.println("Testing " + algo);

    for (int variance = 1; variance <= maxVariances; variance++) {
      double ratioSum = 0;
      double weightSum = 0;
      for (int i = 0; i < 4096; i++) {
        var size = ZOrder3D.indexToCoord(i).add(1, 1, 1);
        Parcel parcel =
            new Parcel(random.nextBlockPos(-100, 100), new Vec3i(size.x, size.y, size.z));

        var testCase = TestCase.create(random, parcel, variance);

        var groups = algo.subdivide(testCase.parcel, testCase, ParcelWithValue::new);
        SubparcelTest.assertParcelEqual(testCase.parcel, groups);

        int volume = parcel.getVolume();
        var countRatio = (double) groups.size() / volume;
        ratioSum += countRatio * volume;
        weightSum += volume;
      }
      var ratio = ratioSum / weightSum;
      System.out.printf("Variance: %d, Average rate: %.2f\n", variance, ratio);
    }
  }

  @Test
  void testSubdivide() {
    testAlgo(SubdivideAlgo.V1, 8);
    testAlgo(SubdivideAlgo.V2, 8);
    testAlgo(SubdivideAlgo.V3, 8);
  }
}
