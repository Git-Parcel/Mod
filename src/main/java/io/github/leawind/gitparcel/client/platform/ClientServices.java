package io.github.leawind.gitparcel.client.platform;

import io.github.leawind.gitparcel.common.platform.api.ClientNetworking;
import java.util.ServiceLoader;

/** Lazily loads client-only platform services without initializing them on dedicated servers. */
public final class ClientServices {
  private ClientServices() {}

  private static final class Holder {
    private static final ClientNetworking NETWORKING =
        ServiceLoader.load(ClientNetworking.class)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Failed to load client networking service"));
  }

  public static ClientNetworking networking() {
    return Holder.NETWORKING;
  }
}
