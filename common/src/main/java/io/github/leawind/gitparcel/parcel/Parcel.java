package io.github.leawind.gitparcel.parcel;


public final class Parcel {

  /** Custom exception for parcel-related errors. */
  public static class ParcelException extends RuntimeException {
    public ParcelException(String message) {
      super(message);
    }

    public ParcelException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
