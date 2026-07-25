package io.github.leawind.gitparcel.common.api.exceptions;

import io.github.leawind.gitparcel.common.api.parcel.ParcelFormat;

/** Custom exception for parcel-related errors. */
public class ParcelException extends Exception {
  public ParcelException(String message) {
    super(message);
  }

  public ParcelException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Exception thrown when a parcel format is not supported */
  public static class UnsupportedFormat extends ParcelException {
    public final ParcelFormat.Spec formatSpec;

    public UnsupportedFormat(ParcelFormat.Spec formatSpec) {
      super(String.format("Unsupported format: %s", formatSpec));
      this.formatSpec = formatSpec;
    }
  }

  /** Exception thrown when a parcel format has fatal errors that cannot be recovered from */
  public static class CorruptedParcelException extends ParcelException {
    public CorruptedParcelException(String message) {
      super(message);
    }

    public CorruptedParcelException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
