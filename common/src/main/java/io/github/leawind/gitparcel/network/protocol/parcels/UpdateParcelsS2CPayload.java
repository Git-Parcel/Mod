package io.github.leawind.gitparcel.network.protocol.parcels;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.world.Parcel;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelsS2CPayload(
    List<Parcel> parcels, List<UUID> removedUuids, boolean isFullSync)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcel.identifier("update_parcels");
  public static final CustomPacketPayload.Type<UpdateParcelsS2CPayload> TYPE =
      new CustomPacketPayload.Type<>(ID);
  public static final Codec<UpdateParcelsS2CPayload> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Parcel.CODEC
                          .listOf()
                          .fieldOf("parcels")
                          .forGetter(UpdateParcelsS2CPayload::parcels),
                      UUIDUtil.CODEC
                          .listOf()
                          .fieldOf("removed_uuids")
                          .forGetter(UpdateParcelsS2CPayload::removedUuids),
                      Codec.BOOL
                          .fieldOf("isFullSync")
                          .forGetter(UpdateParcelsS2CPayload::isFullSync))
                  .apply(instance, UpdateParcelsS2CPayload::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelsS2CPayload> STREAM_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsS2CPayload.CODEC);

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static UpdateParcelsS2CPayload fullSync(List<Parcel> parcels) {
    return new UpdateParcelsS2CPayload(parcels, List.of(), true);
  }

  public static UpdateParcelsS2CPayload incremental(List<Parcel> parcels) {
    return new UpdateParcelsS2CPayload(parcels, List.of(), false);
  }

  public static UpdateParcelsS2CPayload incremental(Parcel parcel) {
    return new UpdateParcelsS2CPayload(List.of(parcel), List.of(), false);
  }

  public static UpdateParcelsS2CPayload incrementalWithRemovals(
      List<Parcel> parcels, List<UUID> removedUuids) {
    return new UpdateParcelsS2CPayload(parcels, removedUuids, false);
  }

  public static UpdateParcelsS2CPayload removalsOnly(List<UUID> removedUuids) {
    return new UpdateParcelsS2CPayload(List.of(), removedUuids, false);
  }

  /** Client-Only */
  public static void handle(UpdateParcelsS2CPayload payload, LocalPlayer localPlayer) {
    if (payload.isFullSync) {
      GitParcelClient.PARCELS.clear();
    } else {
      for (var uuid : payload.removedUuids) {
        GitParcelClient.PARCELS.remove(uuid);
      }
    }

    for (var parcel : payload.parcels) {
      GitParcelClient.PARCELS.put(parcel.uuid(), parcel);
    }
  }
}
