import type { OperationDto } from "../api/types";

/**
 * Operations submitted from this session, remembered so their terminal states
 * can be reported to the user exactly once.
 */
const tracked = new Map<string, { kind: string }>();

let listener: (() => void) | null = null;

/**
 * Registers a callback fired whenever an operation becomes tracked — the
 * operations feed uses it to poll promptly instead of waiting out the idle
 * interval. Avoids a circular import between the feed and the API client.
 */
export function onOperationTracked(callback: () => void): void {
  listener = callback;
}

/** Remembers a submitted operation once the server confirms its identity. */
export function trackSubmitted<P extends Promise<OperationDto>>(promise: P): P {
  promise.then(
    (operation) => {
      tracked.set(operation.operationId, { kind: operation.kind });
      listener?.();
    },
    () => {}, // request-level failures are surfaced by the caller's own error handling
  );
  return promise;
}

/**
 * Returns tracked operations that have reached a terminal state in the given
 * snapshot, removing them from the watch list.
 */
export function collectSettledTracked(all: OperationDto[]): OperationDto[] {
  const settled = all.filter(
    (operation) =>
      tracked.has(operation.operationId) && !isPendingState(operation.state),
  );
  for (const operation of settled) {
    tracked.delete(operation.operationId);
  }
  return settled;
}

export function trackedCount(): number {
  return tracked.size;
}

function isPendingState(state: OperationDto["state"]): boolean {
  return state === "queued" || state === "running";
}
