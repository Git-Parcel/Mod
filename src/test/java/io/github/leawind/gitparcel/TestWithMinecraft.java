package io.github.leawind.gitparcel;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*? if neoforge {*/
/*import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import net.neoforged.fml.loading.FMLLoader;
import sun.misc.Unsafe;

*/
/*?}*/

public abstract class TestWithMinecraft {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestWithMinecraft.class);

  @BeforeAll
  static void beforeAll() {
    /*? if neoforge {*/
    /*try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      Unsafe unsafe = (Unsafe) field.get(null);

      FMLLoader loader = (FMLLoader) unsafe.allocateInstance(FMLLoader.class);

      Field productionField = FMLLoader.class.getDeclaredField("production");
      productionField.setAccessible(true);
      productionField.setBoolean(loader, false);

      Field currentField = FMLLoader.class.getDeclaredField("current");
      currentField.setAccessible(true);
      @SuppressWarnings("unchecked")
      AtomicReference<FMLLoader> currentRef = (AtomicReference<FMLLoader>) currentField.get(null);
      currentRef.set(loader);
      LOGGER.info("NeoForge FML Loader initialized for testing");
    } catch (Exception e) {
      LOGGER.warn("Failed to initialize NeoForge FML Loader", e);
    }
    */
    /*?}*/

    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    try {
      Bootstrap.bootStrap();
    } catch (Throwable e) {
      LOGGER.warn("Bootstrap.bootStrap() failed, but version is available for tests", e);
    }
  }
}
