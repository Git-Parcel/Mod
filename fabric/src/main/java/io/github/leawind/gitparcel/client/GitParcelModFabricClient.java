package io.github.leawind.gitparcel.client;

import com.mojang.logging.LogUtils;
import io.github.leawind.gitparcel.network.payload.UpdateParcelFormatInfosS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;

public class GitParcelModFabricClient implements ClientModInitializer {
  public static final Logger LOGGER = LogUtils.getLogger();

  @Override
  public void onInitializeClient() {
    GitParcelModClient.init();
    GitParcelModFabricClient.init();
  }

  public static void init() {
    // Register key mappings
    LOGGER.debug("Registering key mappings");
    GitParcelOptions.registerKeyMappings(KeyBindingHelper::registerKeyBinding);

    ClientPlayNetworking.registerGlobalReceiver(
        UpdateParcelFormatInfosS2CPayload.TYPE,
        (payload, context) -> UpdateParcelFormatInfosS2CPayload.handle(payload, context.player()));
  }
}
