import { ApiError } from '../api/client';
import { translateId } from '../i18n';

/** Renders any thrown value as a localized user-facing message. */
export function errorText(error: unknown): string {
  if (error instanceof ApiError) {
    return translateId('apiErrors', error.code);
  }
  return error instanceof Error ? error.message : String(error);
}
