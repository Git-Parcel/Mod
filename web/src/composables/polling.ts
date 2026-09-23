import { onScopeDispose, ref, type Ref } from 'vue';

/**
 * Visibility-aware polling: the callback runs immediately and then on an interval
 * that is re-read on every tick (so a reactive interval can speed up or slow down),
 * paused while the tab is hidden.
 */
export function usePolling(action: () => Promise<unknown>, intervalMs: Ref<number> | number) {
  const paused = ref(false);
  let timer: ReturnType<typeof setTimeout> | null = null;
  let stopped = false;

  function resolveInterval(): number {
    return typeof intervalMs === 'number' ? intervalMs : intervalMs.value;
  }

  async function tick() {
    if (stopped) {
      return;
    }
    if (!paused.value && document.visibilityState === 'visible') {
      try {
        await action();
      } catch {
        // Polling errors are surfaced by the caller's own state.
      }
    }
    if (!stopped) {
      timer = setTimeout(() => void tick(), resolveInterval());
    }
  }

  function runNow() {
    void tick();
  }

  document.addEventListener('visibilitychange', runNow);
  runNow();

  onScopeDispose(() => {
    stopped = true;
    if (timer !== null) {
      clearTimeout(timer);
    }
    document.removeEventListener('visibilitychange', runNow);
  });

  return { paused, runNow };
}
