package io.github.leawind.gitparcel.network.protocol.parcelformat;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelFormatInfosS2CPayload(ClientParcelFormatInfos infos)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcelMod.identifier("update_parcel_formats");
  public static final Type<UpdateParcelFormatInfosS2CPayload> TYPE = new Type<>(ID);

  public static final Codec<UpdateParcelFormatInfosS2CPayload> CODEC =
      ClientParcelFormatInfos.CODEC.xmap(
          UpdateParcelFormatInfosS2CPayload::new, UpdateParcelFormatInfosS2CPayload::infos);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelFormatInfosS2CPayload>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatInfosS2CPayload.CODEC);

  public static UpdateParcelFormatInfosS2CPayload from(ParcelFormatRegistry registry) {
    var infos = new ClientParcelFormatInfos(registry.getSaverInfos(), registry.getLoaderInfos());
    return new UpdateParcelFormatInfosS2CPayload(infos);
  }

  @Override
  public @NonNull Type<UpdateParcelFormatInfosS2CPayload> type() {
    return TYPE;
  }

  /** Client-Only */
  public static void handle(UpdateParcelFormatInfosS2CPayload payload, LocalPlayer localPlayer) {
    GitParcelMod.LOGGER.debug("Update parcel format info: {}", payload.infos());
    GitParcelModClient.PARCEL_FORMAT_INFOS = payload.infos();
  }
}
