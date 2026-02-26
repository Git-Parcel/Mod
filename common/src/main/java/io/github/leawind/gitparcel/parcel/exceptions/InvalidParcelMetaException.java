package io.github.leawind.gitparcel.parcel.exceptions;

/** Exceptions thrown when parsing or validating parcel metadata. */
public class InvalidParcelMetaException extends ParcelException.InvalidParcel {
  public InvalidParcelMetaException(String message) {
    super(message);
  }

  public InvalidParcelMetaException(String message, Throwable cause) {
    super(message, cause);
  }
}
