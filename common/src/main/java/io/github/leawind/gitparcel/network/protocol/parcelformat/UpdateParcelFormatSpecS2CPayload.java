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

public record UpdateParcelFormatSpecS2CPayload(ParcelFormatSpecs specs)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcel.identifier("update_parcel_formats");
  public static final Type<UpdateParcelFormatSpecS2CPayload> TYPE = new Type<>(ID);

  public static final Codec<UpdateParcelFormatSpecS2CPayload> CODEC =
      ParcelFormatSpecs.CODEC.xmap(
          UpdateParcelFormatSpecS2CPayload::new, UpdateParcelFormatSpecS2CPayload::specs);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelFormatSpecS2CPayload>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatSpecS2CPayload.CODEC);

  public static UpdateParcelFormatSpecS2CPayload from(ParcelFormatRegistry registry) {
    var savers = registry.streamSavers().map(ParcelFormat::spec).toList();
    var loaders = registry.streamLoaders().map(ParcelFormat::spec).toList();
    return new UpdateParcelFormatSpecS2CPayload(new ParcelFormatSpecs(savers, loaders));
  }

  @Override
  public @NonNull Type<UpdateParcelFormatSpecS2CPayload> type() {
    return TYPE;
  }

  /** Client-Only */
  public static void handle(UpdateParcelFormatSpecS2CPayload payload, LocalPlayer localPlayer) {
    GitParcel.LOGGER.debug("Update parcel format specs: {}", payload.specs());
    GitParcelClient.PARCEL_FORMAT_SPECS = payload.specs();
  }
}
