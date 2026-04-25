package io.github.leawind.gitparcel.network.protocol.parcelformat;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcel;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.client.GitParcelClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelFormatInfosS2CPayload(ClientParcelFormatInfos infos)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcel.identifier("update_parcel_formats");
  public static final Type<UpdateParcelFormatInfosS2CPayload> TYPE = new Type<>(ID);

  public static final Codec<UpdateParcelFormatInfosS2CPayload> CODEC =
      ClientParcelFormatInfos.CODEC.xmap(
          UpdateParcelFormatInfosS2CPayload::new, UpdateParcelFormatInfosS2CPayload::infos);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelFormatInfosS2CPayload>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatInfosS2CPayload.CODEC);

  public static UpdateParcelFormatInfosS2CPayload from(ParcelFormatRegistry registry) {
    var savers = registry.streamSavers().map(ParcelFormat::info).toList();
    var loaders = registry.streamLoaders().map(ParcelFormat::info).toList();
    return new UpdateParcelFormatInfosS2CPayload(new ClientParcelFormatInfos(savers, loaders));
  }

  @Override
  public @NonNull Type<UpdateParcelFormatInfosS2CPayload> type() {
    return TYPE;
  }

  /** Client-Only */
  public static void handle(UpdateParcelFormatInfosS2CPayload payload, LocalPlayer localPlayer) {
    GitParcel.LOGGER.debug("Update parcel format info: {}", payload.infos());
    GitParcelClient.PARCEL_FORMAT_INFOS = payload.infos();
  }
}
