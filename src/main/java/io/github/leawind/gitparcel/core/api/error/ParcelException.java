package io.github.leawind.gitparcel.core.api.error;

import io.github.leawind.gitparcel.core.api.parcel.ParcelFormat;
import java.util.Arrays;

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

    public UnsupportedFormat(ParcelFormat.Spec spec) {
      super(String.format("Unsupported format: %s", spec));
      this.formatSpec = spec;
    }
  }

  public static class UnsupportedFeature extends ParcelException {
    public final ParcelFormat.Spec formatSpec;
    public final ParcelFormat.Feature[] features;

    public UnsupportedFeature(ParcelFormat.Spec spec, ParcelFormat.Feature... features) {
      super(
          String.format("Unsupported features for format %s: %s", spec, Arrays.toString(features)));
      this.formatSpec = spec;
      this.features = features;
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
