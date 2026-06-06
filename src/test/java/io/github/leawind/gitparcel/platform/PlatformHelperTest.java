package io.github.leawind.gitparcel.platform;

import io.github.leawind.gitparcel.mc.platform.api.Services;
import io.github.leawind.gitparcel.testutils.AbstractMinecraftTest;
import org.junit.jupiter.api.Test;

public class PlatformHelperTest extends AbstractMinecraftTest {

  @Test
  void testGetDataVersion() {
    var dataVersion = Services.PLATFORM_HELPER.getDataVersion();
    System.out.println("Data version: " + dataVersion);
  }
}
