package io.github.leawind.gitparcel.client;

import io.github.leawind.inventory.event.EventEmitter;
import net.minecraft.client.Minecraft;

public class GameClientApi {
  public static final EventEmitter<Minecraft> ON_CLIENT_TICK_START = new EventEmitter<>();
}
