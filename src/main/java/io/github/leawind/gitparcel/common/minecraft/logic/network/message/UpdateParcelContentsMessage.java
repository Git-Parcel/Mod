package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentCapabilities;

/** Replaces the client's view of the server's registered parcel content implementations. */
public record UpdateParcelContentsMessage(ParcelContentCapabilities capabilities)
    implements ServerMessage {
  public static final Codec<UpdateParcelContentsMessage> CODEC =
      ParcelContentCapabilities.CODEC.xmap(
          UpdateParcelContentsMessage::new, UpdateParcelContentsMessage::capabilities);
}
