import type { OperationDto } from "../api/types";

/**
 * Operations submitted from this session, remembered so their terminal states
 * can be reported exactly once — as a notification and, for snapshot ops, as
 * a history refresh on the parcel page that submitted them.
 */
const tracked = new Map<string, { kind: string; since: number }>();
const announced = new Set<string>();

/**
 * Tracked entries older than this are dropped without announcement — their
 * operations were evicted from the recent list (or the request failed) and
 * would never settle from the UI's point of view.
 */
const TRACKED_TTL_MS = 30 * 60 * 1000;

type SettledListener = (operation: OperationDto) => void;
const settledListeners = new Set<SettledListener>();

let listener: (() => void) | null = null;

/**
 * Registers a callback fired whenever an operation becomes tracked — the
 * operations feed uses it to poll promptly instead of waiting out the idle
 * interval. Avoids a circular import between the feed and the API client.
 */
export function onOperationTracked(callback: () => void): void {
  listener = callback;
}

/** Subscribes to terminal states of tracked operations; returns the unsubscribe fn. */
export function onTrackedSettled(callback: SettledListener): () => void {
  settledListeners.add(callback);
  return () => settledListeners.delete(callback);
}

/** Remembers a submitted operation once the server confirms its identity. */
export function trackSubmitted<P extends Promise<OperationDto>>(promise: P): P {
  promise.then(
    (operation) => {
      tracked.set(operation.operationId, {
        kind: operation.kind,
        since: Date.now(),
      });
      listener?.();
    },
    () => {}, // request-level failures are surfaced by the caller's own error handling
  );
  return promise;
}

/**
 * Checks the feed snapshot for tracked operations that reached a terminal
 * state and broadcasts each one to the subscribers exactly once.
 */
export function announceSettledTracked(all: OperationDto[]): void {
  const now = Date.now();
  for (const [id, meta] of tracked) {
    if (now - meta.since > TRACKED_TTL_MS) {
      tracked.delete(id);
    }
  }
  for (const operation of all) {
    if (
      !tracked.has(operation.operationId) ||
      announced.has(operation.operationId)
    ) {
      continue;
    }
    if (isPendingState(operation.state)) {
      continue;
    }
    announced.add(operation.operationId);
    tracked.delete(operation.operationId);
    for (const listener of settledListeners) {
      listener(operation);
    }
  }
}

export function trackedCount(): number {
  return tracked.size;
}

function isPendingState(state: OperationDto["state"]): boolean {
  return state === "queued" || state === "running";
}
