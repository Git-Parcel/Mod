package io.github.leawind.gitparcel.common.api.extension.field;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Declares an NBT field that stores a world position, so the built-in declared-field processor
 * rebases it between world space and parcel space on capture and restore.
 *
 * <p>Declared fields are parcel-frame references: they are translated (and mirrored/rotated) with
 * the parcel when it is restored or imported at a different transform. Data that references the
 * wider world instead of the parcel should travel as an attachment, not as a coordinate field.
 *
 * <p>Paths use dot-separated segments. Each segment addresses a compound key or {@code []} to
 * iterate every element of a list, for example {@code flower_pos} or {@code Items[].tag.target}.
 */
public record ParcelCoordinateField(
    Target target, Optional<Identifier> type, String path, Encoding encoding) {
  private static final String PATH_PATTERN = "^[A-Za-z0-9_.:\\[\\]-]+$";

  public enum Target {
    ENTITY,
    BLOCK_ENTITY
  }

  public enum Encoding {
    /** {@link net.minecraft.core.BlockPos#CODEC} form, used by modern vanilla block positions. */
    BLOCK_POS,
    /** Compound with integer {@code X}, {@code Y}, {@code Z} keys, a common modding convention. */
    BLOCK_POS_XYZ,
    /** List of three doubles, the {@link net.minecraft.world.phys.Vec3#CODEC} form. */
    POSITION
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
    return new ParcelCoordinateField(target, Optional.empty(), path, encoding);
  }

  /** Convenience constructor scoped to one entity or block-entity type. */
  public static ParcelCoordinateField forType(
      Target target, Identifier type, String path, Encoding encoding) {
    return new ParcelCoordinateField(target, Optional.of(type), path, encoding);
  }

  public boolean appliesTo(Target queryTarget, @Nullable Identifier queryType) {
    return target == queryTarget
        && (type.isEmpty() || type.orElseThrow().equals(queryType));
  }
}
