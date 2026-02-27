package io.github.leawind.gitparcel.algorithms.subdivide;

import io.github.leawind.gitparcel.parcel.Parcel;
import io.github.leawind.gitparcel.parcel.formats.parcella.SubparcelTest;
import io.github.leawind.gitparcel.parcel.formats.parcella.ZOrder3D;
import io.github.leawind.gitparcel.testutils.RandomForMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.junit.jupiter.api.Test;

public class SubdivideAlgoImplV1Test {
  record TestCase(Parcel parcel, int[][][] valuesArray) {
    int get(BlockPos pos) {
      return valuesArray[pos.getX() - parcel.originX][pos.getY() - parcel.originY][
          pos.getZ() - parcel.originZ];
    }

    static TestCase create(RandomForMC random, Parcel parcel, int valueVariance) {
      int[][][] values = new int[parcel.sizeX][parcel.sizeY][parcel.sizeZ];
      for (var pos : BlockPos.betweenClosed(1, 1, 1, parcel.sizeX, parcel.sizeY, parcel.sizeZ)) {
        values[pos.getX() - 1][pos.getY() - 1][pos.getZ() - 1] = random.nextInt(0, valueVariance);
      }
      return new TestCase(parcel, values);
    }
  }

  @Test
  void testSubdivide() {
    var random = new RandomForMC(12138);

    for (int variance = 2; variance <= 5; variance++) {

      double ratioSum = 0;
      double weightSum = 0;
      for (int i = 0; i < 4096; i++) {
        var size = ZOrder3D.indexToCoord(i).add(1, 1, 1);
        Parcel parcel =
            new Parcel(random.nextBlockPos(-100, 100), new Vec3i(size.x, size.y, size.z));
        int volume = parcel.getVolume();

        for (int j = 0; j < Math.log(volume) + 1; j++) {
          var testCase = TestCase.create(random, parcel, 5);

          var microparcels =
              SubdivideImplV1.INSTANCE.subdivide(
                  testCase.parcel, testCase::get, SubdivideAlgo.ParcelWithValue::new);
          SubparcelTest.assertParcelEqual(testCase.parcel, microparcels);

          var countRatio = (double) microparcels.size() / volume;
          ratioSum += countRatio * volume;
          weightSum += volume;
        }
      }
      var ratio = ratioSum / weightSum;
      System.out.printf("Variance: %d, Average count ratio: %.2f\n", variance, ratio);
    }
  }
}
