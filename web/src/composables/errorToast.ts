import { useMessage } from "naive-ui";
import { errorText } from "../utils/errors";

/**
 * Runs an async action, showing its failure as a localized toast and
 * resolving to undefined instead of throwing — the standard shape for
 * user-triggered button actions across the views.
 */
export function useErrorToast() {
  const message = useMessage();

  return async function run<T>(
    action: () => Promise<T>,
  ): Promise<T | undefined> {
    try {
      return await action();
    } catch (error) {
      message.error(errorText(error));
      return undefined;
    }
  };
}
