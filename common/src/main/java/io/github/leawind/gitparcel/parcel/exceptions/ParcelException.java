package io.github.leawind.gitparcel.parcel.exceptions;

/** Custom exception for parcel-related errors. */
public class ParcelException extends Exception {
  public ParcelException(String message) {
    super(message);
  }

  public ParcelException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Exception thrown when a parcel format is invalid */
  public static class InvalidParcel extends ParcelException {
    public InvalidParcel(String message) {
      super(message);
    }

    public InvalidParcel(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
