package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.api.parcel.ParcelFormatCapabilities;

/** Replaces the client's view of the server's supported parcel formats. */
public record UpdateParcelFormatsMessage(ParcelFormatCapabilities capabilities)
    implements ServerMessage {
  public static final Codec<UpdateParcelFormatsMessage> CODEC =
      ParcelFormatCapabilities.CODEC.xmap(
          UpdateParcelFormatsMessage::new, UpdateParcelFormatsMessage::capabilities);
}
