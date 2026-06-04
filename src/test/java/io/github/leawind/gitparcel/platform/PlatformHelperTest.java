package io.github.leawind.gitparcel.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.gitparcel.TestWithMinecraft;
import io.github.leawind.gitparcel.platform.api.PlatformHelper;
import org.junit.jupiter.api.Test;

public class PlatformHelperTest extends TestWithMinecraft {
  @Test
  void testSimple() {
    assertTrue(true);
  }

  @Test
  void testGetDataVersion() {
    var dataVersion = PlatformHelper.INSTANCE.getDataVersion();
    System.out.println("Data version: " + dataVersion);
  }
}
