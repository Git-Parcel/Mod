package io.github.leawind.gitparcel.server.minecraft.logic.storage.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jspecify.annotations.Nullable;

/** Validation and process-local credentials for shared Git remotes. */
public final class GitRemoteAccess {
  public static final String USERNAME_ENV = "GITPARCEL_GIT_USERNAME";
  public static final String TOKEN_ENV = "GITPARCEL_GIT_TOKEN";

  private GitRemoteAccess() {}

  /**
   * Accepts public HTTPS repository URLs without embedded credentials.
   *
   * <p>SSH support needs an explicit host-key and key-management design, while file and other local
   * schemes would expose the server filesystem to command operators.
   */
  public static String validateRemoteUrl(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Remote URL must not be blank");
    }

    try {
      URI uri = new URI(value).normalize();
      String scheme = uri.getScheme();
      if (scheme == null || !scheme.toLowerCase(Locale.ROOT).equals("https")) {
        throw new IllegalArgumentException("Only HTTPS Git remotes are supported");
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new IllegalArgumentException("Remote URL must include a host");
      }
      if (uri.getUserInfo() != null) {
        throw new IllegalArgumentException("Remote URL must not contain credentials");
      }
      if (uri.getQuery() != null || uri.getFragment() != null) {
        throw new IllegalArgumentException("Remote URL must not contain a query or fragment");
      }
      if (uri.getPath() == null || uri.getPath().isBlank() || uri.getPath().equals("/")) {
        throw new IllegalArgumentException("Remote URL must include a repository path");
      }
      return uri.toASCIIString();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid remote URL", e);
    }
  }

  /**
   * Builds a credentials provider from environment variables without persisting the token.
   *
   * <p>If no token is configured, JGit performs anonymous HTTPS operations.
   */
  public static @Nullable CredentialsProvider credentialsFromEnvironment() {
    String token = System.getenv(TOKEN_ENV);
    if (token == null || token.isBlank()) {
      return null;
    }
    String username = System.getenv(USERNAME_ENV);
    if (username == null || username.isBlank()) {
      username = "git";
    }
    return new UsernamePasswordCredentialsProvider(username, token);
  }
}
