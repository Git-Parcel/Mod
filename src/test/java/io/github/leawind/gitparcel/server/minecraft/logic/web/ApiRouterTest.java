package io.github.leawind.gitparcel.server.minecraft.logic.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApiRouterTest {
  private static Optional<ApiRouter.Match> match(String method, String path) {
    return ApiRouter.match(method, path);
  }

  @Test
  void matchesLiteralRoutes() {
    assertEquals(ApiRouter.Route.STATUS, match("GET", "/api/status").orElseThrow().route());
    assertEquals(ApiRouter.Route.PLAYERS, match("GET", "/api/players").orElseThrow().route());
    assertEquals(
        ApiRouter.Route.PARCELS_BATCH_DELETE,
        match("POST", "/api/parcels/batch-delete").orElseThrow().route());
    assertEquals(
        ApiRouter.Route.REPOSITORY_CLONE,
        match("POST", "/api/repositories/myrepo/clone").orElseThrow().route());
  }

  @Test
  void capturesPathParameters() {
    var match = match("GET", "/api/parcels/6f0a2c1e-1111-2222-3333-444455556666/history").orElseThrow();
    assertEquals(ApiRouter.Route.PARCEL_HISTORY, match.route());
    assertEquals("6f0a2c1e-1111-2222-3333-444455556666", match.params().get("uuid"));

    var operation = match("GET", "/api/operations/abc").orElseThrow();
    assertEquals(ApiRouter.Route.OPERATION_GET, operation.route());
    assertEquals("abc", operation.params().get("uuid"));

    var recover = match("POST", "/api/operations/abc/recover").orElseThrow();
    assertEquals(ApiRouter.Route.OPERATION_RECOVER, recover.route());
    assertEquals("abc", recover.params().get("uuid"));
  }

  @Test
  void methodMustMatch() {
    assertTrue(match("DELETE", "/api/status").isEmpty());
    assertTrue(match("POST", "/api/parcels/xyz/config").isPresent());
    assertTrue(match("GET", "/api/parcels/xyz/config").isEmpty());
  }

  @Test
  void unknownPathsDoNotMatch() {
    assertTrue(match("GET", "/api/unknown").isEmpty());
    assertTrue(match("GET", "/api/parcels/xyz/config/extra").isEmpty());
    assertTrue(match("GET", "/other/status").isEmpty());
    assertTrue(match("GET", "/status").isEmpty());
  }
}
