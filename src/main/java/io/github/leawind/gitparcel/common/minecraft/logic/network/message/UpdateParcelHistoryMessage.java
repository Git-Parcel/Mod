package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.api.git.ParcelHistoryPage;

/** Delivers one requested page of parcel Git history. */
public record UpdateParcelHistoryMessage(ParcelHistoryPage page) implements ServerMessage {
  public static final Codec<UpdateParcelHistoryMessage> CODEC =
      ParcelHistoryPage.CODEC.xmap(
          UpdateParcelHistoryMessage::new, UpdateParcelHistoryMessage::page);
}
