package io.github.leawind.gitparcel.network.payload;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.client.ClientParcelFormatInfos;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelFormatInfosS2CPacket(ClientParcelFormatInfos infos)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcelMod.identifier("update_parcel_formats");
  public static final Type<UpdateParcelFormatInfosS2CPacket> TYPE = new Type<>(ID);

  public static final Codec<UpdateParcelFormatInfosS2CPacket> CODEC =
      ClientParcelFormatInfos.CODEC.xmap(
          UpdateParcelFormatInfosS2CPacket::new, UpdateParcelFormatInfosS2CPacket::infos);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelFormatInfosS2CPacket>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatInfosS2CPacket.CODEC);

  public static UpdateParcelFormatInfosS2CPacket from(ParcelFormatRegistry registry) {
    var infos = new ClientParcelFormatInfos(registry.getSaverInfos(), registry.getLoaderInfos());
    return new UpdateParcelFormatInfosS2CPacket(infos);
  }

  @Override
  public @NonNull Type<UpdateParcelFormatInfosS2CPacket> type() {
    return TYPE;
  }

  /** Client-Only */
  public static void handle(UpdateParcelFormatInfosS2CPacket payload, LocalPlayer localPlayer) {
    GitParcelMod.LOGGER.info("Update parcel format info: {}", payload.infos());
    GitParcelModClient.PARCEL_FORMAT_INFOS = payload.infos();
  }
}
