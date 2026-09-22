package io.github.leawind.gitparcel.server.minecraft.logic.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Per-server lifecycle for the web console.
 *
 * <p>At most one console runs per server instance. A fresh 256-bit session token is minted on
 * every start and dies with the service, so a restarted console never inherits an old token.
 */
public final class WebServiceManager {
  /** Classpath root of the bundled web console bundle, produced by the Gradle web build. */
  private static final String STATIC_ROOT = "gitparcel/web/";

  private static final ConcurrentHashMap<MinecraftServer, WebServiceManager> INSTANCES =
      new ConcurrentHashMap<>();

  private final MinecraftServer server;
  private @Nullable WebService service;

  private WebServiceManager(MinecraftServer server) {
    this.server = server;
  }

  public static WebServiceManager get(MinecraftServer server) {
    return INSTANCES.computeIfAbsent(server, WebServiceManager::new);
  }

  /** Stops any running console when the server shuts down. */
  public static void shutdown(MinecraftServer server) {
    var manager = INSTANCES.remove(server);
    if (manager != null) {
      manager.stop();
    }
  }

  /** Starts a new console; fails if one is already running for this server. */
  public synchronized WebService start(InetSocketAddress address) throws IOException {
    if (service != null) {
      throw new IllegalStateException("Web console is already running");
    }
    var token = newToken();
    service = WebService.start(address, token, new WebApi(server), STATIC_ROOT);
    return service;
  }

  public synchronized void stop() {
    var running = service;
    service = null;
    if (running != null) {
      running.close();
    }
  }

  public synchronized Optional<WebService> service() {
    return Optional.ofNullable(service);
  }

  private static String newToken() {
    var bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
