package io.github.leawind.gitparcel.server.minecraft.logic.web;

/**
 * API-level failure with a stable machine-readable code rendered as {@code {"error": code}}.
 *
 * <p>Codes are part of the API contract and are localized by the web frontend.
 */
public final class ApiException extends RuntimeException {
  private final int status;
  private final String code;

  public ApiException(int status, String code) {
    super(code);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }
}
