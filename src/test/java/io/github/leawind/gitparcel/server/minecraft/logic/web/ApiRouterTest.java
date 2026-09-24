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
  void matchesEveryDocumentedRoute() {
    assertMatches("GET", "/api/parcels", ApiRouter.Route.PARCELS_LIST);
    assertMatches("POST", "/api/parcels", ApiRouter.Route.PARCELS_CREATE);
    assertMatches("GET", "/api/parcels/x", ApiRouter.Route.PARCEL_GET);
    assertMatches("DELETE", "/api/parcels/x", ApiRouter.Route.PARCEL_DELETE);
    assertMatches("POST", "/api/parcels/x/config", ApiRouter.Route.PARCEL_CONFIG);
    assertMatches("POST", "/api/parcels/x/resize", ApiRouter.Route.PARCEL_RESIZE);
    assertMatches("POST", "/api/parcels/x/save", ApiRouter.Route.PARCEL_SAVE);
    assertMatches("GET", "/api/parcels/x/history", ApiRouter.Route.PARCEL_HISTORY);
    assertMatches("POST", "/api/parcels/x/restore", ApiRouter.Route.PARCEL_RESTORE);
    assertMatches("POST", "/api/parcels/x/teleport", ApiRouter.Route.PARCEL_TELEPORT);
    assertMatches("POST", "/api/parcels/x/publish", ApiRouter.Route.PARCEL_PUBLISH);
    assertMatches("POST", "/api/import", ApiRouter.Route.IMPORT);
    assertMatches("GET", "/api/operations", ApiRouter.Route.OPERATIONS_LIST);
    assertMatches("GET", "/api/operations/x", ApiRouter.Route.OPERATION_GET);
    assertMatches("POST", "/api/operations/x/recover", ApiRouter.Route.OPERATION_RECOVER);
    assertMatches("GET", "/api/repositories", ApiRouter.Route.REPOSITORIES_LIST);
    assertMatches("GET", "/api/repositories/x/paths", ApiRouter.Route.REPOSITORY_PATHS);
    assertMatches("POST", "/api/repositories", ApiRouter.Route.REPOSITORY_CREATE);
    assertMatches("POST", "/api/repositories/x/clone", ApiRouter.Route.REPOSITORY_CLONE);
    assertMatches("POST", "/api/repositories/x/fetch", ApiRouter.Route.REPOSITORY_FETCH);
    assertMatches("POST", "/api/repositories/x/pull", ApiRouter.Route.REPOSITORY_PULL);
    assertMatches("POST", "/api/repositories/x/push", ApiRouter.Route.REPOSITORY_PUSH);
  }

  private static void assertMatches(String method, String path, ApiRouter.Route expected) {
    assertEquals(expected, match(method, path).orElseThrow().route());
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
