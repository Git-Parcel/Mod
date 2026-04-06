package io.github.leawind.gitparcel.network.protocol.parcels;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.client.GitParcelClient;
import io.github.leawind.gitparcel.world.Parcel;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelsS2CPayload(List<Parcel> parcels) implements CustomPacketPayload {
  public static final Identifier ID = GitParcel.identifier("update_parcels");
  public static final CustomPacketPayload.Type<UpdateParcelsS2CPayload> TYPE =
      new CustomPacketPayload.Type<>(ID);
  public static final Codec<UpdateParcelsS2CPayload> CODEC =
      Parcel.CODEC.listOf().xmap(UpdateParcelsS2CPayload::new, UpdateParcelsS2CPayload::parcels);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelsS2CPayload> STREAM_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsS2CPayload.CODEC);

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static UpdateParcelsS2CPayload from(List<Parcel> parcels) {
    return new UpdateParcelsS2CPayload(parcels);
  }

  /** Client-Only */
  public static void handle(UpdateParcelsS2CPayload payload, LocalPlayer localPlayer) {
    Object2ObjectMap<UUID, Parcel> map = new Object2ObjectArrayMap<>(2);
    for (var parcel : payload.parcels) {
      map.put(parcel.uuid(), parcel);
    }
    GitParcelClient.PARCELS = map;
  }
}
