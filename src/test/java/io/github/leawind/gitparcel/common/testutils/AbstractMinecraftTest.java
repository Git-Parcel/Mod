package io.github.leawind.gitparcel.common.testutils;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Minimal base class for tests that need Minecraft runtime classes but don't need
 * ParcelContentTypeRegistry or other GitParcel-specific initialization.
 */
public class AbstractMinecraftTest {
  protected GitParcelRandom random;

  @BeforeAll
  static void beforeAll() {
    bootstrapMinecraft();
  }

  @BeforeEach
  void beforeEach() {
    random = new GitParcelRandom(12138);
  }

  private static void bootstrapMinecraft() {


    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    Bootstrap.bootStrap();
  }
}
