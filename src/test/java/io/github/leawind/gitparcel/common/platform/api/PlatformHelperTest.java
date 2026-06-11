package io.github.leawind.gitparcel.common.platform.api;

import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.common.testutils.AbstractMinecraftTest;
import org.junit.jupiter.api.Test;

public class PlatformHelperTest extends AbstractMinecraftTest {

  @Test
  void testGetDataVersion() {
    var dataVersion = Services.PLATFORM_HELPER.getDataVersion();
    System.out.println("Data version: " + dataVersion);
  }
}
