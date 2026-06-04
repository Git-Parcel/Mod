package io.github.leawind.gitparcel;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

public abstract class TestWithMinecraft {
  @BeforeAll
  static void beforeAll() {
    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    Bootstrap.bootStrap();
  }
}
