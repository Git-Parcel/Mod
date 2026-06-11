package io.github.leawind.gitparcel.common.minecraft.logic.network.protocol.parcelformat;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.client.impl.GitParcelClientImpl;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatRegistry;
import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public record UpdateParcelFormatSpecS2CPayload(ParcelFormatSpecs specs)
    implements CustomPacketPayload {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final Identifier ID = GitParcelUtils.identifier("update_parcel_formats");
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
    LOGGER.debug("Update parcel format specs: {}", payload.specs());
    GitParcelClientImpl.INSTANCE.setParcelFormatSpecs(payload.specs());
  }
}
