package io.github.leawind.gitparcel.client.minecraft.logic;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.platform.api.Services;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;

public final class GitParcelClientOptions {
  private GitParcelClientOptions() {}

  public static final KeyMapping keyDebugScreen =
      new KeyMapping("key.gitparcel.debug_screen", InputConstants.KEY_R, KeyMapping.Category.DEBUG);

  public static final KeyMapping keyAdminScreen =
      new KeyMapping(
          "key.gitparcel.admin_screen", InputConstants.KEY_RSHIFT, KeyMapping.Category.CREATIVE);

  public static void registerKeyMappings(Consumer<KeyMapping> registrar) {
    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      registrar.accept(keyDebugScreen);
    }

    registrar.accept(keyAdminScreen);
  }
}
