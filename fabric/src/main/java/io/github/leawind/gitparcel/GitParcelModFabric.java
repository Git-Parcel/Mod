package io.github.leawind.gitparcel;

import com.mojang.brigadier.CommandDispatcher;
import io.github.leawind.gitparcel.network.payload.UpdateParcelFormatInfosS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class GitParcelModFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    GitParcelMod.init();
    GitParcelModFabric.init();
  }

  public static void init() {
    registerPackets();

    registerEvents();
  }

  private static void registerEvents() {
    CommandRegistrationCallback.EVENT.register(GitParcelModFabric::registerCommands);
  }

  private static void registerCommands(
      CommandDispatcher<CommandSourceStack> dispatcher,
      CommandBuildContext context,
      Commands.CommandSelection commandSelection) {
    GitParcelMod.registerCommands(dispatcher, commandSelection, context);
  }

  // NOW forge, neoforge
  private static void registerPackets() {
    PayloadTypeRegistry.playS2C()
        .register(
            UpdateParcelFormatInfosS2CPacket.TYPE, UpdateParcelFormatInfosS2CPacket.STREAM_CODEC);
  }
}
