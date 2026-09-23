package io.github.leawind.gitparcel.server.minecraft.logic.web;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Pure request-path matcher for the JSON API.
 *
 * <p>Kept free of Minecraft and server types so route resolution can be unit tested in isolation;
 * {@link WebApi} switches on the matched {@link Route}.
 */
public final class ApiRouter {
  /** Every API route; segment placeholders use {@code {name}} syntax. */
  public enum Route {
    STATUS("GET", "status"),
    PARCELS_LIST("GET", "parcels"),
    PARCELS_CREATE("POST", "parcels"),
    PARCELS_BATCH_DELETE("POST", "parcels", "batch-delete"),
    PARCEL_GET("GET", "parcels", "{uuid}"),
    PARCEL_DELETE("DELETE", "parcels", "{uuid}"),
    PARCEL_CONFIG("POST", "parcels", "{uuid}", "config"),
    PARCEL_RESIZE("POST", "parcels", "{uuid}", "resize"),
    PARCEL_SAVE("POST", "parcels", "{uuid}", "save"),
    PARCEL_HISTORY("GET", "parcels", "{uuid}", "history"),
    PARCEL_RESTORE("POST", "parcels", "{uuid}", "restore"),
    PARCEL_TELEPORT("POST", "parcels", "{uuid}", "teleport"),
    PARCEL_PUBLISH("POST", "parcels", "{uuid}", "publish"),
    IMPORT("POST", "import"),
    PLAYERS("GET", "players"),
    OPERATIONS_LIST("GET", "operations"),
    OPERATION_GET("GET", "operations", "{uuid}"),
    REPOSITORIES_LIST("GET", "repositories"),
    REPOSITORY_CREATE("POST", "repositories"),
    REPOSITORY_CLONE("POST", "repositories", "{name}", "clone"),
    REPOSITORY_FETCH("POST", "repositories", "{name}", "fetch"),
    REPOSITORY_PULL("POST", "repositories", "{name}", "pull"),
    REPOSITORY_PUSH("POST", "repositories", "{name}", "push");

    private final String method;
    private final String[] segments;

    Route(String method, String... segments) {
      this.method = method;
      this.segments = segments;
    }
  }

  /** A matched route with the captured path parameters. */
  public record Match(Route route, Map<String, String> params) {}

  private record Pattern(String method, String[] segments, Route route) {}

  private static final List<Pattern> PATTERNS =
      List.of(Route.values()).stream().map(ApiRouter::pattern).toList();

  private ApiRouter() {}

  /**
   * Matches an API request path such as {@code /api/parcels/<uuid>/history}. Literal segments take
   * precedence over placeholders.
   */
  public static Optional<Match> match(String method, String path) {
    var segments = path.split("/");
    // Strip the leading empty segment from the absolute "/api/..." form.
    if (segments.length == 0 || !segments[0].isEmpty() || !segments[1].equals("api")) {
      return Optional.empty();
    }
    var relative = new String[segments.length - 2];
    System.arraycopy(segments, 2, relative, 0, relative.length);

    for (var pattern : PATTERNS) {
      if (!pattern.method().equals(method)
          || pattern.segments().length != relative.length) {
        continue;
      }
      var params = matchSegments(pattern.segments(), relative);
      if (params != null) {
        return Optional.of(new Match(pattern.route(), params));
      }
    }
    return Optional.empty();
  }

  private static Map<String, String> matchSegments(String[] expected, String[] actual) {
    Map<String, String> params = null;
    for (int i = 0; i < expected.length; i++) {
      if (expected[i].startsWith("{")) {
        if (params == null) {
          params = new HashMap<>();
        }
        params.put(expected[i].substring(1, expected[i].length() - 1), actual[i]);
      } else if (!expected[i].equals(actual[i])) {
        return null;
      }
    }
    return params == null ? Map.of() : params;
  }

  private static Pattern pattern(Route route) {
    return new Pattern(route.method.toUpperCase(Locale.ROOT), route.segments, route);
  }
}
