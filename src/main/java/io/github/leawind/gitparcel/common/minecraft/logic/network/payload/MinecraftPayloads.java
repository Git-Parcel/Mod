package io.github.leawind.gitparcel.common.minecraft.logic.network.payload;

import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ClientMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelContentsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelHistoryMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateSharedRepositoriesMessage;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * Adapts stable Git Parcel messages to Minecraft's custom-payload API.
 *
 * <p>Payload identifiers, wrappers, and stream codecs are intentionally kept in this one
 * version-sensitive class. Business logic and platform service interfaces exchange {@link
 * ServerMessage} instead.
 */
@VersionSensitive("Minecraft custom payload and stream codec API")
public final class MinecraftPayloads {
  public static final Identifier PARCEL_CONTENTS_ID =
      GitParcelUtils.identifier("update_parcel_contents");
  public static final Identifier PARCELS_ID = GitParcelUtils.identifier("update_parcels");
  public static final Identifier SHARED_REPOSITORIES_ID =
      GitParcelUtils.identifier("update_shared_repositories");
  public static final Identifier OPERATIONS_ID =
      GitParcelUtils.identifier("update_operations");
  public static final Identifier QUERY_SERVER_STATE_ID =
      GitParcelUtils.identifier("query_server_state");
  public static final Identifier QUERY_PARCEL_HISTORY_ID =
      GitParcelUtils.identifier("query_parcel_history");
  public static final Identifier PARCEL_HISTORY_ID =
      GitParcelUtils.identifier("update_parcel_history");

  public static final CustomPacketPayload.Type<ParcelContentsPayload> PARCEL_CONTENTS_TYPE =
      new CustomPacketPayload.Type<>(PARCEL_CONTENTS_ID);
  public static final CustomPacketPayload.Type<ParcelsPayload> PARCELS_TYPE =
      new CustomPacketPayload.Type<>(PARCELS_ID);
  public static final CustomPacketPayload.Type<SharedRepositoriesPayload> SHARED_REPOSITORIES_TYPE =
      new CustomPacketPayload.Type<>(SHARED_REPOSITORIES_ID);
  public static final CustomPacketPayload.Type<OperationsPayload> OPERATIONS_TYPE =
      new CustomPacketPayload.Type<>(OPERATIONS_ID);
  public static final CustomPacketPayload.Type<QueryServerStatePayload> QUERY_SERVER_STATE_TYPE =
      new CustomPacketPayload.Type<>(QUERY_SERVER_STATE_ID);
  public static final CustomPacketPayload.Type<QueryParcelHistoryPayload>
      QUERY_PARCEL_HISTORY_TYPE = new CustomPacketPayload.Type<>(QUERY_PARCEL_HISTORY_ID);
  public static final CustomPacketPayload.Type<ParcelHistoryPayload> PARCEL_HISTORY_TYPE =
      new CustomPacketPayload.Type<>(PARCEL_HISTORY_ID);

  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelContentsPayload>
      PARCEL_CONTENTS_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateParcelContentsMessage.CODEC)
              .map(ParcelContentsPayload::new, ParcelContentsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelsPayload> PARCELS_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsMessage.CODEC)
          .map(ParcelsPayload::new, ParcelsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, SharedRepositoriesPayload>
      SHARED_REPOSITORIES_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateSharedRepositoriesMessage.CODEC)
              .map(SharedRepositoriesPayload::new, SharedRepositoriesPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, OperationsPayload>
      OPERATIONS_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateOperationsMessage.CODEC)
              .map(OperationsPayload::new, OperationsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, QueryServerStatePayload>
      QUERY_SERVER_STATE_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(QueryServerStateMessage.CODEC)
              .map(QueryServerStatePayload::new, QueryServerStatePayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, QueryParcelHistoryPayload>
      QUERY_PARCEL_HISTORY_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(QueryParcelHistoryMessage.CODEC)
              .map(QueryParcelHistoryPayload::new, QueryParcelHistoryPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelHistoryPayload>
      PARCEL_HISTORY_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateParcelHistoryMessage.CODEC)
              .map(ParcelHistoryPayload::new, ParcelHistoryPayload::message);

  private MinecraftPayloads() {}

  public static CustomPacketPayload encode(ServerMessage message) {
    if (message instanceof UpdateParcelContentsMessage update) {
      return new ParcelContentsPayload(update);
    }
    if (message instanceof UpdateParcelsMessage update) {
      return new ParcelsPayload(update);
    }
    if (message instanceof UpdateSharedRepositoriesMessage update) {
      return new SharedRepositoriesPayload(update);
    }
    if (message instanceof UpdateOperationsMessage update) {
      return new OperationsPayload(update);
    }
    if (message instanceof UpdateParcelHistoryMessage update) {
      return new ParcelHistoryPayload(update);
    }
    throw new IllegalArgumentException("Unsupported server message: " + message.getClass());
  }

  public static CustomPacketPayload encode(ClientMessage message) {
    if (message instanceof QueryServerStateMessage query) {
      return new QueryServerStatePayload(query);
    }
    if (message instanceof QueryParcelHistoryMessage query) {
      return new QueryParcelHistoryPayload(query);
    }
    throw new IllegalArgumentException("Unsupported client message: " + message.getClass());
  }

  public record ParcelContentsPayload(UpdateParcelContentsMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<ParcelContentsPayload> type() {
      return PARCEL_CONTENTS_TYPE;
    }
  }

  public record ParcelsPayload(UpdateParcelsMessage message) implements CustomPacketPayload {
    @Override
    public @NonNull Type<ParcelsPayload> type() {
      return PARCELS_TYPE;
    }
  }

  public record SharedRepositoriesPayload(UpdateSharedRepositoriesMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<SharedRepositoriesPayload> type() {
      return SHARED_REPOSITORIES_TYPE;
    }
  }

  public record OperationsPayload(UpdateOperationsMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<OperationsPayload> type() {
      return OPERATIONS_TYPE;
    }
  }

  public record QueryServerStatePayload(QueryServerStateMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<QueryServerStatePayload> type() {
      return QUERY_SERVER_STATE_TYPE;
    }
  }

  public record QueryParcelHistoryPayload(QueryParcelHistoryMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<QueryParcelHistoryPayload> type() {
      return QUERY_PARCEL_HISTORY_TYPE;
    }
  }

  public record ParcelHistoryPayload(UpdateParcelHistoryMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<ParcelHistoryPayload> type() {
      return PARCEL_HISTORY_TYPE;
    }
  }
}
