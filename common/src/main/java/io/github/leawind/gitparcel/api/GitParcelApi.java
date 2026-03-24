package io.github.leawind.gitparcel.api;

import io.github.leawind.gitparcel.world.gitparcel.Parcel;
import io.github.leawind.inventory.event.EventEmitter;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public final class GitParcelApi {
  public static class Events {
    public static final EventEmitter<UdpateParcelsEvent> ON_UPDATE_PARCELS = new EventEmitter<>();

    public record UdpateParcelsEvent(ServerLevel level, List<Parcel> list) {}
  }
}
