package io.github.leawind.gitparcel.common.api.operation;

import java.util.OptionalLong;

/** Thread-safe, non-blocking progress sink for a long-running operation. */
@FunctionalInterface
public interface ProgressReporter {
  ProgressReporter NONE = update -> {};

  void report(Update update);

  default void report(String phase, long completed, String unit) {
    report(new Update(phase, completed, OptionalLong.empty(), unit));
  }

  default void report(String phase, long completed, long total, String unit) {
    report(new Update(phase, completed, OptionalLong.of(total), unit));
  }

  /** Prevents an observational progress callback from breaking the operation it observes. */
  static ProgressReporter safe(ProgressReporter delegate) {
    if (delegate == null || delegate == NONE) {
      return NONE;
    }
    return update -> {
      try {
        delegate.report(update);
      } catch (RuntimeException ignored) {
        // Progress is best effort by contract.
      }
    };
  }

  /** Adds a phase prefix while preserving totals and keeping the delegate failure-isolated. */
  static ProgressReporter prefixed(String prefix, ProgressReporter delegate) {
    if (prefix == null || prefix.isBlank()) {
      throw new IllegalArgumentException("Progress phase prefix must not be blank");
    }
    ProgressReporter safe = safe(delegate);
    if (safe == NONE) {
      return NONE;
    }
    return update ->
        safe.report(
            new Update(
                prefix + update.phase(), update.completed(), update.total(), update.unit()));
  }

  record Update(String phase, long completed, OptionalLong total, String unit) {
    public Update {
      if (phase == null || phase.isBlank()) {
        throw new IllegalArgumentException("Progress phase must not be blank");
      }
      if (completed < 0) {
        throw new IllegalArgumentException("Progress must not be negative");
      }
      total = total == null ? OptionalLong.empty() : total;
      if (total.isPresent() && (total.getAsLong() < completed || total.getAsLong() < 0)) {
        throw new IllegalArgumentException("Progress total must be at least completed");
      }
      unit = unit == null ? "" : unit;
    }
  }
}
