package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Requests fresh, permission-filtered state for server-owned Git workflows. */
public record QueryServerStateMessage(boolean repositories, boolean operations)
    implements ClientMessage {
  public static final Codec<QueryServerStateMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.BOOL
                          .fieldOf("repositories")
                          .forGetter(QueryServerStateMessage::repositories),
                      Codec.BOOL
                          .fieldOf("operations")
                          .forGetter(QueryServerStateMessage::operations))
                  .apply(instance, QueryServerStateMessage::new));

  public static QueryServerStateMessage all() {
    return new QueryServerStateMessage(true, true);
  }
}
