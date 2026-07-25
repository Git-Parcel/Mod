package io.github.leawind.gitparcel.common.minecraft.logic.network.message;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import io.github.leawind.gitparcel.common.api.world.Parcels;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Applies a full or incremental update to the client's parcel collection. */
public record UpdateParcelsMessage(Parcels parcels, Set<UUID> removedUuids, boolean fullSync)
    implements ServerMessage {
  public static final Codec<UpdateParcelsMessage> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Parcels.CODEC.fieldOf("parcels").forGetter(UpdateParcelsMessage::parcels),
                      UUIDUtil.CODEC
                          .listOf()
                          .<Set<UUID>>xmap(HashSet::new, List::copyOf)
                          .fieldOf("removed_uuids")
                          .forGetter(UpdateParcelsMessage::removedUuids),
                      Codec.BOOL
                          .fieldOf("isFullSync")
                          .forGetter(UpdateParcelsMessage::fullSync))
                  .apply(instance, UpdateParcelsMessage::new));

  public UpdateParcelsMessage {
    removedUuids = Set.copyOf(removedUuids);
  }

  public void applyTo(Parcels target) {
    if (fullSync) {
      target.clear();
    } else {
      target.removeAll(removedUuids);
    }
    target.putAll(parcels);
  }

  public static UpdateParcelsMessage fullSync(Parcels parcels) {
    return new UpdateParcelsMessage(parcels, Set.of(), true);
  }

  public static UpdateParcelsMessage incremental(Parcel parcel) {
    return new UpdateParcelsMessage(Parcels.singleton(parcel), Set.of(), false);
  }

  public static UpdateParcelsMessage incrementalWithRemovals(
      List<Parcel> parcels, Collection<UUID> removedUuids) {
    return new UpdateParcelsMessage(new Parcels(parcels), Set.copyOf(removedUuids), false);
  }

  public static UpdateParcelsMessage removals(Collection<UUID> removedUuids) {
    return new UpdateParcelsMessage(new Parcels(), Set.copyOf(removedUuids), false);
  }
}
