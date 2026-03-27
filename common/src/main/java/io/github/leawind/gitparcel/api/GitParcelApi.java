package io.github.leawind.gitparcel.api;

import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import io.github.leawind.inventory.event.EventEmitter;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class GitParcelApi {
  public static class Events {
    public static final EventEmitter<UpdateParcelsEvent> ON_PARCELS_UPDATE = new EventEmitter<>();

    public record UpdateParcelsEvent(ServerLevel level, List<Parcel> parcels) {}

    public static final EventEmitter<UpdateParcelEvent> ON_PARCEL_UPDATE = new EventEmitter<>();

    public record UpdateParcelEvent(ServerLevel level, Parcel parcel) {}
  }
}
