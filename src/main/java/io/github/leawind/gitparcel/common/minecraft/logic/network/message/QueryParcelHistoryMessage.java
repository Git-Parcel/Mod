package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Requests one cursor-based page of snapshot history for a parcel in the player's current level. */
public record QueryParcelHistoryMessage(
    UUID parcelUuid, Optional<SnapshotId> cursor, int limit) implements ClientMessage {
  public static final int MAX_LIMIT = 100;

  public static final Codec<QueryParcelHistoryMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC
                          .fieldOf("parcel_uuid")
                          .forGetter(QueryParcelHistoryMessage::parcelUuid),
                      SnapshotId.CODEC
                          .optionalFieldOf("cursor")
                          .forGetter(QueryParcelHistoryMessage::cursor),
                      Codec.intRange(1, MAX_LIMIT)
                          .fieldOf("limit")
                          .forGetter(QueryParcelHistoryMessage::limit))
                  .apply(instance, QueryParcelHistoryMessage::new));

  public QueryParcelHistoryMessage {
    cursor = cursor == null ? Optional.empty() : cursor;
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new IllegalArgumentException("History page limit must be between 1 and " + MAX_LIMIT);
    }
  }
}
