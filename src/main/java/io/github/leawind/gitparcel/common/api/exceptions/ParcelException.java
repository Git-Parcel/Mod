package io.github.leawind.gitparcel.common.api.exceptions;

import io.github.leawind.gitparcel.common.api.parcel.content.ParcelContentType;

/** Custom exception for parcel-related errors. */
public class ParcelException extends Exception {
  public ParcelException(String message) {
    super(message);
  }

  public ParcelException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Exception thrown when a snapshot references an unavailable parcel content implementation. */
  public static class UnsupportedContent extends ParcelException {
    public final ParcelContentType.Spec contentSpec;

    public UnsupportedContent(ParcelContentType.Spec contentSpec) {
      super("Unsupported parcel content: " + contentSpec);
      this.contentSpec = contentSpec;
    }
  }

  /** Exception thrown when parcel content has fatal errors that cannot be recovered from. */
  public static class CorruptedParcelException extends ParcelException {
    public CorruptedParcelException(String message) {
      super(message);
    }

    public CorruptedParcelException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Another write use case currently owns the parcel. */
  public static class Busy extends ParcelException {
    public Busy(String message) {
      super(message);
    }
  }
}
