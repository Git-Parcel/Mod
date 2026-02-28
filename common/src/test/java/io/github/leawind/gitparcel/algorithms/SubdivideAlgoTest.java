package io.github.leawind.gitparcel.algorithms;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.SubparcelTest;
import io.github.leawind.gitparcel.parcel.formats.parcella.ZOrder3D;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class SubdivideAlgoTest {

  public static final class TestedValues implements SubdivideAlgo.Values {
    private final Parcel parcel;
    private final int[][][] valuesArray;

    public TestedValues(Parcel parcel, int valueVariance, RandomForMC random) {
      this.parcel = parcel;
      valuesArray = new int[parcel.sizeX][parcel.sizeY][parcel.sizeZ];
      for (var pos : BlockPos.betweenClosed(1, 1, 1, parcel.sizeX, parcel.sizeY, parcel.sizeZ)) {
        valuesArray[pos.getX() - 1][pos.getY() - 1][pos.getZ() - 1] =
            random.nextInt(0, valueVariance);
      }
    }

    @Override
    public int get(int x, int y, int z) {
      try {

        return valuesArray[x - parcel.originX][y - parcel.originY][z - parcel.originZ];
      } catch (IndexOutOfBoundsException e) {
        throw e;
      }
    }
  }

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

  static void testAlgo(String name, SubdivideAlgo algo, int maxVariances) {
    var random = new RandomForMC(12138);

    System.out.println("Testing " + name);

    for (int variance = 1; variance <= maxVariances; variance++) {
      double ratioSum = 0;
      double weightSum = 0;
      for (int i = 0; i < 4096; i++) {
        var size = ZOrder3D.indexToCoord(i).add(1, 1, 1);
        Parcel parcel =
            new Parcel(random.nextBlockPos(-100, 100), new Vec3i(size.x, size.y, size.z));

        var testCase = new TestedValues(parcel, variance, random);

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
    for (var field : SubdivideAlgo.class.getDeclaredFields()) {
      if (field.getType() == SubdivideAlgo.class && field.canAccess(null)) {
        String name = field.getName();
        if (name.startsWith("_")) continue;
        try {
          testAlgo(name, (SubdivideAlgo) field.get(null), 8);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
