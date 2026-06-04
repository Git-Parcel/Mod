package io.github.leawind.gitparcel.core.api.error;

/** Exceptions thrown when parsing or validating parcel metadata. */
public class InvalidParcelMetaException extends ParcelException.CorruptedParcelException {
  public InvalidParcelMetaException(String message) {
    super(message);
  }

  public InvalidParcelMetaException(String message, Throwable cause) {
    super(message, cause);
  }
}
