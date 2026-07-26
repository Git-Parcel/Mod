package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Requests one cursor-based page of Git history for a parcel in the player's current level. */
public record QueryParcelHistoryMessage(
    UUID parcelUuid, Optional<String> beforeRevision, int limit) implements ClientMessage {
  public static final int MAX_LIMIT = 50;

  public static final Codec<QueryParcelHistoryMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC
                          .fieldOf("parcel_uuid")
                          .forGetter(QueryParcelHistoryMessage::parcelUuid),
                      Codec.STRING
                          .optionalFieldOf("before_revision")
                          .forGetter(QueryParcelHistoryMessage::beforeRevision),
                      Codec.intRange(1, MAX_LIMIT)
                          .fieldOf("limit")
                          .forGetter(QueryParcelHistoryMessage::limit))
                  .apply(instance, QueryParcelHistoryMessage::new));

  public QueryParcelHistoryMessage {
    beforeRevision = beforeRevision == null ? Optional.empty() : beforeRevision;
    if (beforeRevision.isPresent() && beforeRevision.orElseThrow().isBlank()) {
      throw new IllegalArgumentException("History cursor must not be blank");
    }
    if (limit < 1 || limit > MAX_LIMIT) {
      throw new IllegalArgumentException("History page limit must be between 1 and " + MAX_LIMIT);
    }
  }
}
