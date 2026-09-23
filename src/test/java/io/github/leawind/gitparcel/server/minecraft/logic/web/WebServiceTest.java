package io.github.leawind.gitparcel.server.minecraft.logic.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests against a real {@link WebService} on an ephemeral port.
 *
 * <p>Static resources resolve against the test classpath, where a fixture index.html is
 * provided; the API handler is a stub, so these tests stay independent of Minecraft types.
 */
class WebServiceTest {
  private static final String TOKEN = "0123456789abcdef0123456789abcdef";

  private final HttpClient client = HttpClient.newHttpClient();

  private WebService service;
  private URI baseUri;

  @BeforeEach
  void setUp() throws IOException {
    service =
        WebService.start(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            TOKEN,
            request ->
                "/api/status".equals(request.path()) && "GET".equals(request.method())
                    ? Optional.of(WebService.WebResponse.json(200, "{\"ok\":true}"))
                    : Optional.empty(),
            "gitparcel/web/");
    baseUri = URI.create("http://127.0.0.1:" + service.address().getPort());
  }

  @AfterEach
  void tearDown() {
    service.close();
  }

  private HttpResponse<String> get(String path, Map<String, String> headers) throws Exception {
    var builder = HttpRequest.newBuilder(baseUri.resolve(path)).GET();
    headers.forEach(builder::header);
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void apiRejectsMissingToken() throws Exception {
    var response = get("/api/status", Map.of());
    assertEquals(401, response.statusCode());
    assertEquals("{\"error\":\"unauthorized\"}", response.body());
  }

  @Test
  void apiRejectsWrongToken() throws Exception {
    var response = get("/api/status", Map.of("Authorization", "Bearer deadbeef"));
    assertEquals(401, response.statusCode());
  }

  @Test
  void apiAcceptsBearerToken() throws Exception {
    var response = get("/api/status", Map.of("Authorization", "Bearer " + TOKEN));
    assertEquals(200, response.statusCode());
    assertEquals("{\"ok\":true}", response.body());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("json"));
  }

  @Test
  void apiAcceptsQueryToken() throws Exception {
    var response = get("/api/status?token=" + TOKEN, Map.of());
    assertEquals(200, response.statusCode());
  }

  @Test
  void unknownApiRouteIsNotFound() throws Exception {
    var response = get("/api/unknown?token=" + TOKEN, Map.of());
    assertEquals(404, response.statusCode());
    assertEquals("{\"error\":\"not_found\"}", response.body());
  }

  @Test
  void rootServesIndexHtml() throws Exception {
    var response = get("/", Map.of());
    assertEquals(200, response.statusCode());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/html"));
    assertTrue(response.body().contains("gitparcel web test fixture"));
  }

  @Test
  void spaRouteFallsBackToIndexHtml() throws Exception {
    var response = get("/parcels/list", Map.of());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("gitparcel web test fixture"));
  }

  @Test
  void traversalPathIsRejected() throws Exception {
    // Encoded dot segments are what actually reaches a server: plain ".." is normalized
    // away by browsers and rejected outright by java.net.http before sending.
    assertEquals(400, get("/a/..%2Fb", Map.of()).statusCode());
    assertEquals(400, get("/%2e%2e/web", Map.of()).statusCode());
    assertEquals(400, get("/assets/%2e%2e%5Csecret", Map.of()).statusCode());
  }
}
