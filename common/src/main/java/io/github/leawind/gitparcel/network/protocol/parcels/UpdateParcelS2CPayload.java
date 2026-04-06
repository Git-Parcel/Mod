package io.github.leawind.gitparcel.network.protocol.parcels;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.world.Parcel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelS2CPayload(Parcel parcel) implements CustomPacketPayload {
  public static final Identifier ID = GitParcelMod.identifier("update_parcel");
  public static final CustomPacketPayload.Type<UpdateParcelS2CPayload> TYPE =
      new CustomPacketPayload.Type<>(ID);
  public static final Codec<UpdateParcelS2CPayload> CODEC =
      Parcel.CODEC.xmap(UpdateParcelS2CPayload::new, UpdateParcelS2CPayload::parcel);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelS2CPayload> STREAM_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelS2CPayload.CODEC);

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static UpdateParcelS2CPayload from(Parcel parcel) {
    return new UpdateParcelS2CPayload(parcel);
  }

  /** Client-Only */
  public static void handle(UpdateParcelS2CPayload payload, LocalPlayer localPlayer) {
    GitParcelModClient.PARCELS.put(payload.parcel.uuid(), payload.parcel);
  }
}
