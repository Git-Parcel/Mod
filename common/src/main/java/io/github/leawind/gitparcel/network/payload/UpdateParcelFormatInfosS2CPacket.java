package io.github.leawind.gitparcel.network.payload;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.client.ClientParcelFormatInfos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelFormatInfosS2CPacket(ClientParcelFormatInfos formats)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcelMod.identifier("update_parcel_formats");
  public static final Type<UpdateParcelFormatInfosS2CPacket> TYPE = new Type<>(ID);

  public static final Codec<UpdateParcelFormatInfosS2CPacket> CODEC =
      ClientParcelFormatInfos.CODEC.xmap(
          UpdateParcelFormatInfosS2CPacket::new, UpdateParcelFormatInfosS2CPacket::formats);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelFormatInfosS2CPacket>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatInfosS2CPacket.CODEC);

  @Override
  public @NonNull Type<UpdateParcelFormatInfosS2CPacket> type() {
    return TYPE;
  }
}
