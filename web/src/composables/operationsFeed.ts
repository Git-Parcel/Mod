import { ref } from "vue";
import { api } from "../api/client";
import type { OperationDto } from "../api/types";
import { isOperationActive } from "../utils/format";
import { onOperationTracked, trackedCount } from "./trackedOperations";

/**
 * Single shared feed of recent operations.
 *
 * The console polls this endpoint from two consumers (the operations page and
 * the completion notifier); one feed keeps one request stream and one state.
 * Polling runs on a self-rescheduling timeout so the interval can adapt:
 * fast while operations are in flight, relaxed once everything settled.
 */
const operations = ref<OperationDto[]>([]);
const failed = ref(false);

let started = false;
let busy = false;
let timer: ReturnType<typeof setTimeout> | null = null;

const ACTIVE_INTERVAL_MS = 3000;
const IDLE_INTERVAL_MS = 30000;

function currentIntervalMs(): number {
  const busy_ = trackedCount() > 0 ||
    operations.value.some((operation) => isOperationActive(operation));
  return busy_ ? ACTIVE_INTERVAL_MS : IDLE_INTERVAL_MS;
}

async function refresh(): Promise<void> {
  if (busy) return;
  busy = true;
  try {
    operations.value = (await api.operations(100)).operations;
    failed.value = false;
  } catch {
    failed.value = true;
  } finally {
    busy = false;
  }
}

function tick(): Promise<void> {
  if (document.visibilityState === "visible") {
    return refresh().then(schedule);
  }
  schedule();
  return Promise.resolve();
}

function schedule(): void {
  if (timer !== null) clearTimeout(timer);
  timer = setTimeout(() => {
    timer = null;
    void tick();
  }, currentIntervalMs());
}

/** Idempotently starts the shared feed; safe to call from every consumer. */
export function startOperationsFeed(): void {
  if (started) return;
  started = true;
  // A just-submitted operation short-circuits the idle interval so its
  // completion notification does not wait out a slow tick.
  onOperationTracked(() => {
    if (document.visibilityState === "visible") void tick();
  });
  void tick();
}

export function useOperationsFeed(): {
  operations: typeof operations;
  failed: typeof failed;
  refresh: typeof refresh;
} {
  startOperationsFeed();
  return { operations, failed, refresh };
}
