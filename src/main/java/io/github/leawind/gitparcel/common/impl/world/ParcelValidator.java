package io.github.leawind.gitparcel.common.impl.world;

import io.github.leawind.gitparcel.common.api.world.Parcel;
import java.util.Collection;

/** Domain validation for adding a parcel to a level. */
public final class ParcelValidator {
  private ParcelValidator() {}

  /**
   * Validates constraints that depend on the other parcels in a level.
   *
   * @throws IllegalArgumentException if the parcel cannot be added
   */
  public static void validateNewParcel(
      Parcel parcel, Collection<Parcel> existingParcels, long maxParcelVolume) {
    var size = parcel.meta().size();
    if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
      throw new IllegalArgumentException("Parcel size must be positive: " + size);
    }

    long volume;
    try {
      volume =
          Math.multiplyExact(
              Math.multiplyExact((long) size.getX(), size.getY()), (long) size.getZ());
    } catch (ArithmeticException ignored) {
      volume = Long.MAX_VALUE;
    }

    if (volume >= maxParcelVolume) {
      throw new IllegalArgumentException(
          "Parcel is too big: %d >= %d".formatted(volume, maxParcelVolume));
    }

    for (var existing : existingParcels) {
      if (existing.uuid().equals(parcel.uuid())) {
        throw new IllegalArgumentException(
            "Parcel with uuid %s already exists".formatted(parcel.uuid()));
      }
    }

    for (var existing : existingParcels) {
      if (parcel.getBoundingBox().intersects(existing.getBoundingBox())) {
        throw new IllegalArgumentException(
            "The new parcel intersects with existing parcel: %s <> %s"
                .formatted(parcel.uuid(), existing.uuid()));
      }
    }
  }
}
