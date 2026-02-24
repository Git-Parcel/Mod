package io.github.leawind.gitparcel.parcel.formats.parcella;

import org.joml.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class TestParcellaV0 {
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
