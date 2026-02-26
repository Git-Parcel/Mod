package io.github.leawind.gitparcel.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

public class ParcelTest {
  @Test
  void testMaxPos() {
    assertEquals(new BlockPos(15, 15, 15), new Parcel(0, 0, 0, 16, 16, 16).getMaxPos());
    assertEquals(new BlockPos(0, 8, 49), new Parcel(-15, -7, 18, 16, 16, 32).getMaxPos());
  }
}
