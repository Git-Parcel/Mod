package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotTreePage;

/** Delivers one requested page of the parcel's logical snapshot tree. */
public record UpdateParcelHistoryMessage(SnapshotTreePage page) implements ServerMessage {
  public static final Codec<UpdateParcelHistoryMessage> CODEC =
      SnapshotTreePage.CODEC.xmap(
          UpdateParcelHistoryMessage::new, UpdateParcelHistoryMessage::page);
}
