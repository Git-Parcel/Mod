/**
 * Typed access to the mod's `/api` JSON endpoints.
 *
 * The session token arrives once via the entry URL emitted by
 * `/parcel web start` (`/?token=...`), is captured into sessionStorage and
 * sent as a bearer header afterwards.
 */

const TOKEN_STORAGE_KEY = 'gitparcel-token';

export interface Status {
  minecraft: { name: string; dataVersion: number };
  gitparcel: { version: string };
  onlinePlayers: number;
  maxPlayers: number;
  parcels: Array<{ dimension: string; count: number }>;
  operations: { active: number; retained: number };
  serverTime: string;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(`API request failed (${status}): ${code}`);
    this.name = 'ApiError';
  }
}

/** Stores the token from the entry URL, if any, and strips it from the address bar. */
export function captureTokenFromUrl(): void {
  const token = new URLSearchParams(window.location.search).get('token');
  if (token) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
    const url = new URL(window.location.href);
    url.searchParams.delete('token');
    window.history.replaceState(null, '', url);
  }
}

export function hasToken(): boolean {
  return sessionStorage.getItem(TOKEN_STORAGE_KEY) !== null;
}

async function request<T>(path: string): Promise<T> {
  const headers: Record<string, string> = {};
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const response = await fetch(path, { headers });
  if (!response.ok) {
    let code = 'http_error';
    try {
      const body = (await response.json()) as { error?: string };
      if (body.error) code = body.error;
    } catch {
      // Non-JSON error body; keep the generic code.
    }
    throw new ApiError(response.status, code);
  }
  return (await response.json()) as T;
}

export function fetchStatus(): Promise<Status> {
  return request<Status>('/api/status');
}
