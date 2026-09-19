package io.github.leawind.gitparcel.common.minecraft.logic.network.payload;

import io.github.leawind.gitparcel.common.impl.GitParcelUtils;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.ServerMessage;
import io.github.leawind.gitparcel.common.minecraft.logic.network.message.UpdateParcelsMessage;
import io.github.leawind.gitparcel.common.utils.anno.VersionSensitive;
/*? if >=26.1 {*/
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;
/*?} else {*/
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
 *//*?}*/
import net.minecraft.resources.Identifier;

/**
 * Adapts stable Git Parcel messages to Minecraft's play-packet API.
 *
 * <p>Payload identifiers, wrappers, and stream codecs are intentionally kept in this one
 * version-sensitive class. Business logic and platform service interfaces exchange {@link
 * ServerMessage} instead.
 */
@VersionSensitive("Minecraft custom payload and stream codec API")
public final class MinecraftPayloads {
  /*? if >=26.1 {*/
  public static final Identifier PARCELS_ID = GitParcelUtils.identifier("update_parcels");

  public static final CustomPacketPayload.Type<ParcelsPayload> PARCELS_TYPE =
      new CustomPacketPayload.Type<>(PARCELS_ID);

  public static final StreamCodec<RegistryFriendlyByteBuf, ParcelsPayload> PARCELS_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsMessage.CODEC)
          .map(ParcelsPayload::new, ParcelsPayload::message);

  private MinecraftPayloads() {}

  public static CustomPacketPayload encode(ServerMessage message) {
    if (message instanceof UpdateParcelsMessage update) {
      return new ParcelsPayload(update);
    }
    throw new IllegalArgumentException("Unsupported server message: " + message.getClass());
  }

  public record ParcelsPayload(UpdateParcelsMessage message) implements CustomPacketPayload {
    @Override
    public @NonNull Type<ParcelsPayload> type() {
      return PARCELS_TYPE;
    }
  }
  /*?} else {*/
  /*public static final Identifier PARCELS_ID = GitParcelUtils.identifier("update_parcels");

  private MinecraftPayloads() {}

  public static void write(UpdateParcelsMessage message, FriendlyByteBuf buf) {
    buf.writeNbt(
        (CompoundTag)
            UpdateParcelsMessage.CODEC.encodeStart(NbtOps.INSTANCE, message).result().orElseThrow());
  }

  public static UpdateParcelsMessage read(FriendlyByteBuf buf) {
    return UpdateParcelsMessage.CODEC
        .parse(NbtOps.INSTANCE, buf.readNbt())
        .result().orElseThrow();
  }

  public static UpdateParcelsMessage encode(ServerMessage message) {
    if (message instanceof UpdateParcelsMessage update) {
      return update;
    }
    throw new IllegalArgumentException("Unsupported server message: " + message.getClass());
  }*/
  /*?}*/
}
