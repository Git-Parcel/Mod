package io.github.leawind.gitparcel.network.protocol.parcelinstance;

import com.mojang.serialization.Codec;
import io.github.leawind.gitparcel.GitParcelMod;
import io.github.leawind.gitparcel.client.GitParcelModClient;
import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelInstancesS2CPayload(List<ParcelInstance> parcelInstances)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcelMod.identifier("update_parcel_instances");
  public static final CustomPacketPayload.Type<UpdateParcelInstancesS2CPayload> TYPE =
      new CustomPacketPayload.Type<>(ID);
  public static final Codec<UpdateParcelInstancesS2CPayload> CODEC =
      ParcelInstance.CODEC
          .listOf()
          .xmap(
              UpdateParcelInstancesS2CPayload::new,
              UpdateParcelInstancesS2CPayload::parcelInstances);

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelInstancesS2CPayload>
      STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(UpdateParcelInstancesS2CPayload.CODEC);

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static UpdateParcelInstancesS2CPayload from(List<ParcelInstance> parcelInstances) {
    return new UpdateParcelInstancesS2CPayload(parcelInstances);
  }

  /** Client-Only */
  public static void handle(UpdateParcelInstancesS2CPayload payload, LocalPlayer localPlayer) {
    GitParcelModClient.PARCEL_INSTANCES = payload.parcelInstances;
  }
}
