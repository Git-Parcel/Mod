import { type Ref, ref } from "vue";

/**
 * Shared shape of the list/detail pages: nullable data, a failed flag for the
 * page-level alert, and a refresh action to wire into `usePolling`.
 *
 * Fetch errors are absorbed into `failed` — polling views surface them once
 * via the alert, not as a toast per retry.
 */
export function useApiData<T>(fetcher: () => Promise<T>): {
  data: Ref<T | null>;
  failed: Ref<boolean>;
  refresh: () => Promise<void>;
} {
  const data: Ref<T | null> = ref(null);
  const failed = ref(false);

  async function refresh() {
    try {
      data.value = await fetcher();
      failed.value = false;
    } catch {
      failed.value = true;
    }
  }

  return { data, failed, refresh };
}
