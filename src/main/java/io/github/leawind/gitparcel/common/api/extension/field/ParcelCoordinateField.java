package io.github.leawind.gitparcel.common.api.extension.field;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Declares an NBT field that stores a world position or orientation, so the built-in declared-field
 * processor rebases it between world space and parcel space on capture and restore.
 *
 * <p>Declared fields are parcel-frame references: they are translated (and mirrored/rotated) with
 * the parcel when it is restored or imported at a different transform. Data that references the
 * wider world instead of the parcel should travel as an attachment, not as a coordinate field.
 *
 * <p>Paths use dot-separated segments. Each segment addresses a compound key or {@code []} to
 * iterate every element of a list, for example {@code flower_pos} or {@code Items[].tag.target}.
 */
public record ParcelCoordinateField(
    Target target,
    Optional<Identifier> type,
    String path,
    Encoding encoding,
    Pointing pointing) {
  private static final String PATH_PATTERN = "^[A-Za-z0-9_.:\\[\\]-]+$";

  public enum Target {
    ENTITY,
    BLOCK_ENTITY
  }

  /**
   * Rule 3.2 inside/outside override for a spatial edge. {@code GEOMETRIC} detects the pointing
   * by containment in the parcel extent; {@code INSIDE} always transforms; {@code OUTSIDE} never
   * transforms. Orientation encodings ({@code DIRECTION}, {@code ROTATION_STEP}) ignore this.
   */
  public enum Pointing {
    GEOMETRIC,
    INSIDE,
    OUTSIDE
  }

  public enum Encoding {
    /** {@link net.minecraft.core.BlockPos#CODEC} form, used by modern vanilla block positions. */
    BLOCK_POS,
    /** Compound with integer {@code X}, {@code Y}, {@code Z} keys, a common modding convention. */
    BLOCK_POS_XYZ,
    /** List of three doubles, the {@link net.minecraft.world.phys.Vec3#CODEC} form. */
    POSITION,
    /**
     * A {@link net.minecraft.core.Direction} stored as a name string or a 3D data value number;
     * transformed as an orientation role and written back in the original form.
     */
    DIRECTION,
    /**
     * An item-frame style in-plane rotation step (45° units) stored as a number; paired with the
     * record's {@link #DIRECTION} field for the frame facing (SEMANTICS.md rule 3.1).
     */
    ROTATION_STEP
  }

  public ParcelCoordinateField {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(encoding, "encoding");
    if (path.isEmpty() || path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
      throw new IllegalArgumentException("Invalid NBT path: " + path);
    }
    if (!path.matches(PATH_PATTERN)) {
      throw new IllegalArgumentException("Invalid NBT path: " + path);
    }
  }

  /** Convenience constructor applying to every type of the target. */
  public static ParcelCoordinateField forAny(
      Target target, String path, Encoding encoding) {
    return forAny(target, path, encoding, Pointing.GEOMETRIC);
  }

  /** Convenience constructor applying to every type of the target, with an explicit pointing. */
  public static ParcelCoordinateField forAny(
      Target target, String path, Encoding encoding, Pointing pointing) {
    return new ParcelCoordinateField(target, Optional.empty(), path, encoding, pointing);
  }

  /** Convenience constructor scoped to one entity or block-entity type. */
  public static ParcelCoordinateField forType(
      Target target, Identifier type, String path, Encoding encoding) {
    return forType(target, type, path, encoding, Pointing.GEOMETRIC);
  }

  /** Convenience constructor scoped to one entity or block-entity type, with an explicit pointing. */
  public static ParcelCoordinateField forType(
      Target target, Identifier type, String path, Encoding encoding, Pointing pointing) {
    return new ParcelCoordinateField(target, Optional.of(type), path, encoding, pointing);
  }

  public boolean appliesTo(Target queryTarget, @Nullable Identifier queryType) {
    return target == queryTarget
        && (type.isEmpty() || type.orElseThrow().equals(queryType));
  }
}
