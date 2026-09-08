package io.github.leawind.gitparcel.common.api.operation;

import com.mojang.serialization.Codec;
import java.util.Locale;

/**
 * Stable machine-readable categories for failed operations. Error text may change at any time;
 * consumers must classify failures with this code instead of matching message strings.
 */
public enum OperationErrorCode {
  /** The bounded operation queue rejected the task before it started; retrying may succeed. */
  QUEUE_FULL,

  /** A server-thread phase did not complete within the callback timeout. */
  SERVER_THREAD_TIMEOUT,

  /** Server shutdown canceled a queued or running task. */
  SHUTDOWN_CANCELED,

  /** Any other failure; consult the error message and server logs. */
  INTERNAL;

  public static final Codec<OperationErrorCode> CODEC =
      Codec.STRING.xmap(
          value -> OperationErrorCode.valueOf(value.toUpperCase(Locale.ROOT)),
          value -> value.name().toLowerCase(Locale.ROOT));
}
