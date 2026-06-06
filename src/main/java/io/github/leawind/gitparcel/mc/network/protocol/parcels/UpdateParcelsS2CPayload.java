package io.github.leawind.gitparcel.mc.network.protocol.parcels;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.core.GitParcel;
import io.github.leawind.gitparcel.core.world.Parcel;
import io.github.leawind.gitparcel.core.world.Parcels;
import io.github.leawind.gitparcel.mc.client.GitParcelClient;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record UpdateParcelsS2CPayload(Parcels parcels, Set<UUID> removedUuids, boolean isFullSync)
    implements CustomPacketPayload {
  public static final Identifier ID = GitParcel.identifier("update_parcels");
  public static final Type<UpdateParcelsS2CPayload> TYPE =
      new Type<>(ID);

  public static final Codec<UpdateParcelsS2CPayload> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Parcels.CODEC.fieldOf("parcels").forGetter(UpdateParcelsS2CPayload::parcels),
                      UUIDUtil.CODEC
                          .listOf()
                          .<Set<UUID>>xmap(HashSet::new, List::copyOf)
                          .fieldOf("removed_uuids")
                          .forGetter(UpdateParcelsS2CPayload::removedUuids),
                      Codec.BOOL
                          .fieldOf("isFullSync")
                          .forGetter(UpdateParcelsS2CPayload::isFullSync))
                  .apply(instance, UpdateParcelsS2CPayload::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, UpdateParcelsS2CPayload> STREAM_CODEC =
      ByteBufCodecs.fromCodecWithRegistries(UpdateParcelsS2CPayload.CODEC);

  @Override
  public @NonNull Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static UpdateParcelsS2CPayload fullSync(Parcels parcels) {
    return new UpdateParcelsS2CPayload(parcels, Set.of(), true);
  }

  public static UpdateParcelsS2CPayload incremental(Parcel parcel) {
    return new UpdateParcelsS2CPayload(Parcels.singleton(parcel), Set.of(), false);
  }

  public static UpdateParcelsS2CPayload incrementalWithRemovals(
      List<Parcel> parcels, Collection<UUID> removedUuids) {
    return new UpdateParcelsS2CPayload(new Parcels(parcels), Set.copyOf(removedUuids), false);
  }

  public static UpdateParcelsS2CPayload removals(Collection<UUID> removedUuids) {
    return new UpdateParcelsS2CPayload(new Parcels(), Set.copyOf(removedUuids), false);
  }

  /** Client-Only */
  public static void handle(UpdateParcelsS2CPayload payload, LocalPlayer localPlayer) {
    if (payload.isFullSync) {
      GitParcelClient.PARCELS.clear();
    } else {
      GitParcelClient.PARCELS.removeAll(payload.removedUuids);
    }

    GitParcelClient.PARCELS.putAll(payload.parcels);
  }
}
