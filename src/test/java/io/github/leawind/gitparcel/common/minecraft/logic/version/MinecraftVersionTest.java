package io.github.leawind.gitparcel.common.minecraft.logic.version;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import org.junit.jupiter.api.Test;

class MinecraftVersionTest extends AbstractMinecraftTest {

  @Test
  void exposesCurrentDataVersion() {
    assertTrue(MinecraftVersion.currentDataVersion() > 0);
  }
}
