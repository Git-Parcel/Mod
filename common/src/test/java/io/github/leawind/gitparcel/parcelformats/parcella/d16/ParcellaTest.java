package io.github.leawind.gitparcel.parcelformats.parcella.d16;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.gitparcel.parcelformats.parcella.Subparcel;
import io.github.leawind.gitparcel.parcelformats.parcella.SubparcelFormat;
import net.minecraft.util.ProblemReporter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParcellaTest {
  private static final Subparcel TEST_SUBPARCEL = new Subparcel(0, 0, 0, 2, 2, 2);
  private static final Logger LOGGER = LoggerFactory.getLogger(ParcellaTest.class);
  private static final ProblemReporter DUMMY_REPORTER = new ProblemReporter.ScopedCollector(LOGGER);

  @Test
  void detectSubparcelFormat() {
    byte[] rle3dData = "000=000\n1\n".getBytes();
    assertEquals(SubparcelFormat.RLE3D, ParcellaD16Loader.detectSubparcelFormat(rle3dData));

    byte[] flatData = "1\n2\n3\n4\n5\n6\n7\n8\n".getBytes();
    assertEquals(SubparcelFormat.FLAT, ParcellaD16Loader.detectSubparcelFormat(flatData));

    byte[] shortData = "123".getBytes();
    assertEquals(SubparcelFormat.FLAT, ParcellaD16Loader.detectSubparcelFormat(shortData));
  }

  @Test
  void loadSubparcelRLE3D() {
    byte[] data = "000=000\n1\n001=001\n2\n".getBytes();

    int[][][] result =
        new ParcellaD16Loader().loadSubparcelRLE3D(TEST_SUBPARCEL, data, DUMMY_REPORTER);

    assertEquals(1, result[0][0][0]);
    assertEquals(2, result[0][0][1]);
  }

  @Test
  void loadSubparcelRLE3DRange() {
    byte[] data = "000111=3\n".getBytes();

    int[][][] result =
        new ParcellaD16Loader().loadSubparcelRLE3D(TEST_SUBPARCEL, data, DUMMY_REPORTER);

    for (int x = 0; x < 2; x++) {
      for (int y = 0; y < 2; y++) {
        for (int z = 0; z < 2; z++) {
          assertEquals(3, result[x][y][z]);
        }
      }
    }
  }

  @Test
  void loadSubparcelFLAT() {
    byte[] data = "1\n2\n3\n4\n5\n6\n7\n8\n".getBytes();

    int[][][] result =
        new ParcellaD16Loader().loadSubparcelFLAT(TEST_SUBPARCEL, data, DUMMY_REPORTER);

    int index = 0;
    for (int z = 0; z < 2; z++) {
      for (int y = 0; y < 2; y++) {
        for (int x = 0; x < 2; x++) {
          assertEquals(index + 1, result[x][y][z]);
          index++;
        }
      }
    }
  }

  @Test
  void loadSubparcelRLE3DInvalid() {
    byte[] data = "000=000\ninvalid\n".getBytes(); // Invalid format

    int[][][] result =
        new ParcellaD16Loader().loadSubparcelRLE3D(TEST_SUBPARCEL, data, DUMMY_REPORTER);
    assertNotNull(result);
  }

  @Test
  void loadSubparcelFLATInvalid() {
    byte[] data = "1\ninvalid\n3\n".getBytes(); // Invalid format

    int[][][] result =
        new ParcellaD16Loader().loadSubparcelFLAT(TEST_SUBPARCEL, data, DUMMY_REPORTER);
    assertNotNull(result);
  }
}
