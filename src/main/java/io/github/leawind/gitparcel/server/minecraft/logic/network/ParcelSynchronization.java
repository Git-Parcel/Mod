package io.github.leawind.gitparcel.server.minecraft.logic.network;

import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.platform.api.Services;
import io.github.leawind.gitparcel.server.minecraft.logic.world.ParcelRegistry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Network edge adapter for parcel registration state. Services stay free of packet handling; every
 * registration change reaches clients through this class.
 */
public final class ParcelSynchronization {
  private ParcelSynchronization() {}

  public static void broadcastFullSync(
      ServerLevel level, io.github.leawind.gitparcel.common.api.world.Parcels parcels) {
    Services.SERVER_NETWORKING.sendToAllPlayers(level, UpdateParcelsMessage.fullSync(parcels));
  }

  public static void broadcastIncremental(ServerLevel level, Parcel parcel) {
    Services.SERVER_NETWORKING.sendToAllPlayers(level, UpdateParcelsMessage.incremental(parcel));
  }

  public static void broadcastRemovals(ServerLevel level, Set<UUID> uuids) {
    Services.SERVER_NETWORKING.sendToAllPlayers(level, UpdateParcelsMessage.removals(uuids));
  }

  /** Sends the authoritative parcel list of the player's level to that player. */
  public static void syncParcelsTo(ServerPlayer player) {
    Services.SERVER_NETWORKING.send(
        player,
        UpdateParcelsMessage.fullSync(ParcelRegistry.get(player.level()).parcelsById()));
  }
}
