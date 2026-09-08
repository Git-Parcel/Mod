package io.github.leawind.gitparcel.common.api.extension.field;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Declares an NBT field that stores an entity UUID, so entity references that point at other
 * entities inside the same parcel are rewritten when restore assigns fresh UUIDs.
 *
 * <p>References to entities outside the parcel (for example a tamed animal's owner) are left
 * untouched. Paths use the same dot-separated syntax as {@link ParcelCoordinateField}; the field
 * must hold a UUID in the {@link net.minecraft.core.UUIDUtil#CODEC} int-array form.
 */
public record ParcelEntityRefField(Optional<Identifier> type, String path) {
  private static final String PATH_PATTERN = "^[A-Za-z0-9_.:\\[\\]-]+$";

  public ParcelEntityRefField {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
    if (path.isEmpty() || path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
      throw new IllegalArgumentException("Invalid NBT path: " + path);
    }
    if (!path.matches(PATH_PATTERN)) {
      throw new IllegalArgumentException("Invalid NBT path: " + path);
    }
  }

  /** Convenience constructor applying to every entity type. */
  public static ParcelEntityRefField forAny(String path) {
    return new ParcelEntityRefField(Optional.empty(), path);
  }

  /** Convenience constructor scoped to one entity type. */
  public static ParcelEntityRefField forType(Identifier type, String path) {
    return new ParcelEntityRefField(Optional.of(type), path);
  }

  public boolean appliesTo(@Nullable Identifier entityType) {
    return type.isEmpty() || type.orElseThrow().equals(entityType);
  }
}
