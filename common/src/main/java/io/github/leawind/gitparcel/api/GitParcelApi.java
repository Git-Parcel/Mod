package io.github.leawind.gitparcel.api;

import io.github.leawind.gitparcel.world.gitparcel.ParcelInstance;
import io.github.leawind.inventory.event.EventEmitter;
import java.util.List;
import net.minecraft.server.level.ServerLevel;

public class GitParcelApi {
  public static final class Events {
    public static final EventEmitter<UdpateParcelInstancesEvent> ON_UPDATE_PARCEL_INSTANCES =
        new EventEmitter<>();

    public record UdpateParcelInstancesEvent(ServerLevel level, List<ParcelInstance> list) {}
  }
}
