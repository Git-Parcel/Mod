package io.github.leawind.gitparcel.common.testutils;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/*? if neoforge {*/
/*import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import sun.misc.Unsafe;

*//*?}*/

/**
 * Minimal base class for tests that need Minecraft runtime classes but don't need
 * ParcelFormatRegistry or other GitParcel-specific initialization.
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

      // Initialize loadingModList to avoid "The loading mod list isn't built yet."
      LoadingModList loadingModList =
          (LoadingModList) unsafe.allocateInstance(LoadingModList.class);
      Field modListField = FMLLoader.class.getDeclaredField("loadingModList");
      modListField.setAccessible(true);
      modListField.set(loader, loadingModList);

      // Populate lists/maps on LoadingModList so FeatureFlagLoader can iterate them
      for (Field f : LoadingModList.class.getDeclaredFields()) {
        if (List.class.isAssignableFrom(f.getType())) {
          f.setAccessible(true);
          f.set(loadingModList, Collections.emptyList());
        } else if (Map.class.isAssignableFrom(f.getType())) {
          f.setAccessible(true);
          f.set(loadingModList, Collections.emptyMap());
        } else if (Set.class.isAssignableFrom(f.getType())) {
          f.setAccessible(true);
          f.set(loadingModList, Collections.emptySet());
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    *//*?}*/

    SharedConstants.setVersion(DetectedVersion.BUILT_IN);
    Bootstrap.bootStrap();
  }
}
