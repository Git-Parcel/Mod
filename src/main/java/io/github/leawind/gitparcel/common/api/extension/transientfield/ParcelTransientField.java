package io.github.leawind.gitparcel.common.api.extension.transientfield;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Declares an NBT field whose value carries no cross-snapshot semantics (SEMANTICS.md definition
 * 2.5): either eliminated on capture ({@link Kind#ELIMINATE}) or rewritten as an offset from the
 * capture-time game time ({@link Kind#OFFSET_GAME_TIME}, rule 2.3).
 *
 * <p>Eliminated fields are absent from snapshots, so restores fall back to the engine defaults;
 * the restore side performs no elimination. Offset fields travel as {@code value - gameTime} and
 * are re-anchored to the restore-time game time.
 */
public record ParcelTransientField(
    Target target, Optional<Identifier> type, String path, Kind kind) {

  public enum Kind {
    ELIMINATE,
    OFFSET_GAME_TIME
  }

  public enum Target {
    ENTITY,
    BLOCK_ENTITY
  }

  public ParcelTransientField {
    if (path.isEmpty() || path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
      throw new IllegalArgumentException("Invalid NBT path: " + path);
    }
  }

  /** Convenience declaration applying to every type of the target. */
  public static ParcelTransientField forAny(Target target, String path, Kind kind) {
    return new ParcelTransientField(target, Optional.empty(), path, kind);
  }

  /** Convenience declaration scoped to one entity or block-entity type. */
  public static ParcelTransientField forType(
      Target target, Identifier type, String path, Kind kind) {
    return new ParcelTransientField(target, Optional.of(type), path, kind);
  }

  public boolean appliesTo(Target queryTarget, @Nullable Identifier queryType) {
    return target == queryTarget
        && (type.isEmpty() || type.orElseThrow().equals(queryType));
  }
}
