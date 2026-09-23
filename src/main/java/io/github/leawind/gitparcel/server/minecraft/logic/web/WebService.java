package io.github.leawind.gitparcel.server.minecraft.logic.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One running web console HTTP endpoint.
 *
 * <p>Routes {@code /api/*} to the token-checked {@link ApiHandler} and everything else to static
 * resources below a classpath root, falling back to {@code index.html} for single-page-application
 * routes. Runs on the JDK's built-in HttpServer with a small daemon pool; Minecraft state is only
 * touched through the handler supplied by the caller.
 */
public final class WebService implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebService.class);

  /** JSON API surface; implemented by the Minecraft-facing layer. */
  public interface ApiHandler {
    /** An API request with its raw JSON body (empty for body-less methods). */
    record Request(String method, String path, Map<String, String> query, byte[] body) {}

    /**
     * @return empty when the method/path pair is not a known API route
     */
    Optional<WebResponse> handle(Request request);
  }

  /** An already rendered HTTP response body. */
  public record WebResponse(int status, String contentType, byte[] body) {
    public static WebResponse json(int status, String json) {
      return new WebResponse(
          status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    public static WebResponse jsonError(int status, String code) {
      return json(status, "{\"error\":\"" + code + "\"}");
    }
  }

  private static final Map<String, String> CONTENT_TYPES =
      Map.ofEntries(
          Map.entry("html", "text/html; charset=utf-8"),
          Map.entry("js", "text/javascript; charset=utf-8"),
          Map.entry("mjs", "text/javascript; charset=utf-8"),
          Map.entry("css", "text/css; charset=utf-8"),
          Map.entry("json", "application/json; charset=utf-8"),
          Map.entry("svg", "image/svg+xml"),
          Map.entry("png", "image/png"),
          Map.entry("jpg", "image/jpeg"),
          Map.entry("ico", "image/x-icon"),
          Map.entry("txt", "text/plain; charset=utf-8"),
          Map.entry("map", "application/json"),
          Map.entry("woff", "font/woff"),
          Map.entry("woff2", "font/woff2"),
          Map.entry("wasm", "application/wasm"));

  private final HttpServer httpServer;
  private final ExecutorService executor;
  private final String tokenHex;
  private final ApiHandler apiHandler;
  private final String staticRoot;

  private WebService(
      HttpServer httpServer,
      ExecutorService executor,
      String tokenHex,
      ApiHandler apiHandler,
      String staticRoot) {
    this.httpServer = httpServer;
    this.executor = executor;
    this.tokenHex = tokenHex;
    this.apiHandler = apiHandler;
    this.staticRoot = staticRoot;
  }

  public static WebService start(
      InetSocketAddress address, String tokenHex, ApiHandler apiHandler, String staticRoot)
      throws IOException {
    var httpServer = HttpServer.create(address, 0);
    var executor = Executors.newFixedThreadPool(2, newDaemonThreadFactory());
    var service = new WebService(httpServer, executor, tokenHex, apiHandler, staticRoot);
    httpServer.createContext("/", service::handle);
    httpServer.setExecutor(executor);
    httpServer.start();
    return service;
  }

  /** The bound address; may differ from the requested one when an ephemeral port was used. */
  public InetSocketAddress address() {
    return httpServer.getAddress();
  }

  /** The hex session token, for building entry URLs. */
  public String tokenHex() {
    return tokenHex;
  }

  private static ThreadFactory newDaemonThreadFactory() {
    var counter = new AtomicInteger();
    return runnable -> {
      var thread = new Thread(runnable, "gitparcel-web-" + counter.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    };
  }

  private void handle(HttpExchange exchange) throws IOException {
    try {
      var rawPath = exchange.getRequestURI().getRawPath();
      if (rawPath.startsWith("/api/") || rawPath.equals("/api")) {
        serveApi(exchange);
      } else {
        serveStatic(exchange);
      }
    } catch (Exception e) {
      LOGGER.error("Web console request failed: {}", exchange.getRequestURI(), e);
      respond(exchange, WebResponse.jsonError(500, "internal_error"));
    } finally {
      exchange.close();
    }
  }

  private void serveApi(HttpExchange exchange) throws IOException {
    try {
      if (!isAuthorized(exchange)) {
        respond(exchange, WebResponse.jsonError(401, "unauthorized"));
        return;
      }
      var uri = exchange.getRequestURI();
      var request =
          new ApiHandler.Request(
              exchange.getRequestMethod(),
              uri.getPath(),
              decodeQuery(uri.getRawQuery()),
              readBody(exchange));
      var response = apiHandler.handle(request);
      respond(exchange, response.orElseGet(() -> WebResponse.jsonError(404, "not_found")));
    } catch (ApiException e) {
      respond(exchange, WebResponse.jsonError(e.status(), e.code()));
    } catch (Exception e) {
      LOGGER.error("Web console request failed: {}", exchange.getRequestURI(), e);
      respond(exchange, WebResponse.jsonError(500, "internal_error"));
    }
  }

  private static final int MAX_BODY_BYTES = 1024 * 1024;

  private static byte[] readBody(HttpExchange exchange) throws IOException {
    try (var in = exchange.getRequestBody()) {
      var body = in.readNBytes(MAX_BODY_BYTES + 1);
      if (body.length > MAX_BODY_BYTES) {
        throw new ApiException(400, "invalid_body");
      }
      return body;
    }
  }

  private boolean isAuthorized(HttpExchange exchange) {
    var header = exchange.getRequestHeaders().getFirst("Authorization");
    if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      if (constantTimeTokenEquals(header.substring(7).trim())) {
        return true;
      }
    }
    return constantTimeTokenEquals(decodeQuery(exchange.getRequestURI().getRawQuery())
        .getOrDefault("token", ""));
  }

  private boolean constantTimeTokenEquals(String candidate) {
    if (candidate.length() != tokenHex.length()) {
      return false;
    }
    return MessageDigest.isEqual(
        candidate.getBytes(StandardCharsets.UTF_8), tokenHex.getBytes(StandardCharsets.UTF_8));
  }

  private static Map<String, String> decodeQuery(@Nullable String rawQuery) {
    var result = new LinkedHashMap<String, String>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return result;
    }
    for (var pair : rawQuery.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int separator = pair.indexOf('=');
      var key = separator < 0 ? pair : pair.substring(0, separator);
      var value = separator < 0 ? "" : pair.substring(separator + 1);
      result.put(urlDecode(key), urlDecode(value));
    }
    return result;
  }

  private void serveStatic(HttpExchange exchange) throws IOException {
    var path = exchange.getRequestURI().getPath();
    // An empty sanitized result is the console root "/" and resolves to the entry point.
    var relative =
        sanitizeStaticPath(path).map(sanitized -> sanitized.isEmpty() ? "index.html" : sanitized).orElse(null);
    if (relative == null) {
      respondText(exchange, 400, "Invalid path");
      return;
    }
    var bytes = readStaticResource(relative);
    if (bytes == null) {
      // Single-page-application routes have no file of their own; unknown asset
      // extensions fall through to the console entry point as well.
      bytes = readStaticResource("index.html");
    }
    if (bytes == null) {
      respondText(
          exchange,
          404,
          "Web console bundle is missing from this build. Run `npm run build` in the web/"
              + " directory (or the gradle buildWebUi task) and rebuild the mod.");
      return;
    }
    respondBytes(exchange, 200, contentTypeOf(relative), bytes);
  }

  /** Decodes and validates a request path into a safe resource path below the static root. */
  private static Optional<String> sanitizeStaticPath(String path) {
    if (path.isEmpty() || path.charAt(0) != '/' || path.indexOf('\\') >= 0) {
      return Optional.empty();
    }
    var decoded = urlDecode(path);
    if (decoded.indexOf('\\') >= 0 || decoded.contains("..")) {
      return Optional.empty();
    }
    var builder = new StringBuilder();
    for (var segment : decoded.split("/")) {
      if (segment.isEmpty() || segment.equals(".")) {
        continue;
      }
      if (builder.length() > 0) {
        builder.append('/');
      }
      builder.append(segment);
    }
    return Optional.of(builder.toString());
  }

  private @Nullable byte[] readStaticResource(String relativePath) throws IOException {
    try (InputStream stream =
        WebService.class.getClassLoader().getResourceAsStream(staticRoot + relativePath)) {
      if (stream == null) {
        return null;
      }
      return stream.readAllBytes();
    }
  }

  private static String contentTypeOf(String relativePath) {
    int dot = relativePath.lastIndexOf('.');
    var extension =
        dot < 0 ? "" : relativePath.substring(dot + 1).toLowerCase(Locale.ROOT);
    return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
  }

  private static String urlDecode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  private void respond(HttpExchange exchange, WebResponse response) throws IOException {
    if (response.body().length == 0) {
      exchange.sendResponseHeaders(response.status(), -1);
      return;
    }
    exchange
        .getResponseHeaders()
        .set("Content-Type", response.contentType());
    exchange.getResponseHeaders().set("Cache-Control", "no-cache");
    exchange.sendResponseHeaders(response.status(), response.body().length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(response.body());
    }
  }

  private void respondBytes(HttpExchange exchange, int status, String contentType, byte[] body)
      throws IOException {
    respond(exchange, new WebResponse(status, contentType, body));
  }

  private void respondText(HttpExchange exchange, int status, String text) throws IOException {
    respond(
        exchange,
        new WebResponse(status, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8)));
  }

  @Override
  public void close() {
    httpServer.stop(0);
    executor.shutdownNow();
  }
}
