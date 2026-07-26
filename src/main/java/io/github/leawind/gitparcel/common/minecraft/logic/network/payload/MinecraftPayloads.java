package io.github.leawind.gitparcel.common.minecraft.logic.network.payload;

import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ClientMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.QueryServerStateMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateGitOperationsMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelFormatsMessage;
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
  public static final Identifier PARCEL_FORMATS_ID =
      GitParcelUtils.identifier("update_parcel_formats");
  public static final Identifier PARCELS_ID = GitParcelUtils.identifier("update_parcels");
  public static final Identifier SHARED_REPOSITORIES_ID =
      GitParcelUtils.identifier("update_shared_repositories");
  public static final Identifier GIT_OPERATIONS_ID =
      GitParcelUtils.identifier("update_git_operations");
  public static final Identifier QUERY_SERVER_STATE_ID =
      GitParcelUtils.identifier("query_server_state");

  public static final CustomPacketPayload.Type<ParcelFormatsPayload> PARCEL_FORMATS_TYPE =
      new CustomPacketPayload.Type<>(PARCEL_FORMATS_ID);
  public static final CustomPacketPayload.Type<ParcelsPayload> PARCELS_TYPE =
      new CustomPacketPayload.Type<>(PARCELS_ID);
  public static final CustomPacketPayload.Type<SharedRepositoriesPayload> SHARED_REPOSITORIES_TYPE =
      new CustomPacketPayload.Type<>(SHARED_REPOSITORIES_ID);
  public static final CustomPacketPayload.Type<GitOperationsPayload> GIT_OPERATIONS_TYPE =
      new CustomPacketPayload.Type<>(GIT_OPERATIONS_ID);
  public static final CustomPacketPayload.Type<QueryServerStatePayload> QUERY_SERVER_STATE_TYPE =
      new CustomPacketPayload.Type<>(QUERY_SERVER_STATE_ID);

  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelFormatsPayload>
      PARCEL_FORMATS_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateParcelFormatsMessage.CODEC)
              .map(ParcelFormatsPayload::new, ParcelFormatsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelsPayload> PARCELS_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsMessage.CODEC)
          .map(ParcelsPayload::new, ParcelsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, SharedRepositoriesPayload>
      SHARED_REPOSITORIES_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateSharedRepositoriesMessage.CODEC)
              .map(SharedRepositoriesPayload::new, SharedRepositoriesPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, GitOperationsPayload>
      GIT_OPERATIONS_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(UpdateGitOperationsMessage.CODEC)
              .map(GitOperationsPayload::new, GitOperationsPayload::message);
  public static final StreamCodec<RegistryFriendlyByteBuf, QueryServerStatePayload>
      QUERY_SERVER_STATE_CODEC =
          ByteBufCodecs.fromCodecWithRegistries(QueryServerStateMessage.CODEC)
              .map(QueryServerStatePayload::new, QueryServerStatePayload::message);

  private MinecraftPayloads() {}

  public static CustomPacketPayload encode(ServerMessage message) {
    if (message instanceof UpdateParcelFormatsMessage update) {
      return new ParcelFormatsPayload(update);
    }
    if (message instanceof UpdateParcelsMessage update) {
      return new ParcelsPayload(update);
    }
    if (message instanceof UpdateSharedRepositoriesMessage update) {
      return new SharedRepositoriesPayload(update);
    }
    if (message instanceof UpdateGitOperationsMessage update) {
      return new GitOperationsPayload(update);
    }
    throw new IllegalArgumentException("Unsupported server message: " + message.getClass());
  }

  public static CustomPacketPayload encode(ClientMessage message) {
    if (message instanceof QueryServerStateMessage query) {
      return new QueryServerStatePayload(query);
    }
    throw new IllegalArgumentException("Unsupported client message: " + message.getClass());
  }

  public record ParcelFormatsPayload(UpdateParcelFormatsMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<ParcelFormatsPayload> type() {
      return PARCEL_FORMATS_TYPE;
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

  public record GitOperationsPayload(UpdateGitOperationsMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<GitOperationsPayload> type() {
      return GIT_OPERATIONS_TYPE;
    }
  }

  public record QueryServerStatePayload(QueryServerStateMessage message)
      implements CustomPacketPayload {
    @Override
    public @NonNull Type<QueryServerStatePayload> type() {
      return QUERY_SERVER_STATE_TYPE;
    }
  }
}
