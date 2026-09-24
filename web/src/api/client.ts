import { ref } from 'vue'
import type { OperationDto, ParcelDto, PlayerDto, RepoDto, Status, TreePageDto } from './types'
import { trackSubmitted } from '../composables/trackedOperations'

/** Set once any request comes back 401; App.vue switches to the re-entry guide. */
export const unauthorized = ref(false)

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
  ) {
    super(`API ${status}: ${code}`)
    this.name = 'ApiError'
  }
}

const TOKEN_STORAGE_KEY = 'gitparcel-token'

/** Captures the token from the entry URL into sessionStorage and strips it from the bar. */
export function captureTokenFromUrl(): void {
  const token = new URLSearchParams(window.location.search).get('token')
  if (token) {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, token)
    const url = new URL(window.location.href)
    url.searchParams.delete('token')
    window.history.replaceState(null, '', url)
  }
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = {}
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'network')
  }

  if (response.status === 401) {
    unauthorized.value = true
    throw new ApiError(401, 'unauthorized')
  }
  if (!response.ok) {
    let code = 'http_error'
    try {
      const payload = (await response.json()) as { error?: string }
      if (payload.error) { code = payload.error }
    } catch {
      // Non-JSON error body; keep the generic code.
    }
    throw new ApiError(response.status, code)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export const api = {
  status: () => request<Status>('GET', '/api/status'),

  parcels: (dimension?: string) =>
    request<{ parcels: ParcelDto[] }>(
      'GET',
      dimension ? `/api/parcels?dimension=${encodeURIComponent(dimension)}` : '/api/parcels',
    ),
  parcel: (uuid: string) => request<ParcelDto>('GET', `/api/parcels/${uuid}`),
  createParcel: (body: {
    dimension: string
    from: [number, number, number]
    to: [number, number, number]
    name: string
    mirror?: string
    rotation?: string
  }) => request<ParcelDto>('POST', '/api/parcels', body),
  batchDeleteParcels: (uuids: string[]) => request<{ count: number }>('POST', '/api/parcels/batch-delete', { uuids }),
  updateConfig: (uuid: string, key: string, value: string | number | boolean) =>
    request<ParcelDto>('POST', `/api/parcels/${uuid}/config`, { key, value }),
  resizeParcel: (
    uuid: string,
    from: [number, number, number],
    to: [number, number, number],
  ) => request<ParcelDto>('POST', `/api/parcels/${uuid}/resize`, { from, to }),
  deleteParcel: (uuid: string) => request<void>('DELETE', `/api/parcels/${uuid}`),
  saveParcel: (uuid: string, name?: string) => {
    const promise = request<OperationDto>(
      'POST',
      `/api/parcels/${uuid}/save`,
      name ? { name } : {},
    )
    return trackSubmitted(promise)
  },
  history: (uuid: string, limit?: number, cursor?: string) => {
    const params = new URLSearchParams()
    if (limit !== undefined) { params.set('limit', String(limit)) }
    if (cursor) { params.set('cursor', cursor) }
    const query = params.toString()
    return request<TreePageDto>(
      'GET',
      `/api/parcels/${uuid}/history${query ? `?${query}` : ''}`,
    )
  },
  restoreParcel: (
    uuid: string,
    snapshotId: string,
    mode: 'direct' | 'save-first',
  ) => {
    const promise = request<OperationDto>(
      'POST',
      `/api/parcels/${uuid}/restore`,
      {
        snapshotId,
        mode,
      },
    )
    return trackSubmitted(promise)
  },
  teleportParcel: (uuid: string, players: string[]) =>
    request<{ count: number }>('POST', `/api/parcels/${uuid}/teleport`, {
      players,
    }),
  publishParcel: (
    uuid: string,
    repository: string,
    path: string,
    message?: string,
  ) => {
    const promise = request<OperationDto>(
      'POST',
      `/api/parcels/${uuid}/publish`,
      {
        repository,
        path,
        message,
      },
    )
    return trackSubmitted(promise)
  },

  importParcel: (body: {
    repository: string
    revision: string
    path: string
    dimension: string
    at: [number, number, number]
    mirror?: string
    rotation?: string
  }) => {
    const promise = request<OperationDto>('POST', '/api/import', body)
    return trackSubmitted(promise)
  },

  players: () => request<{ players: PlayerDto[] }>('GET', '/api/players'),

  operations: (limit?: number) =>
    request<{ operations: OperationDto[] }>(
      'GET',
      limit === undefined ? '/api/operations' : `/api/operations?limit=${limit}`,
    ),
  operation: (uuid: string) => request<OperationDto>('GET', `/api/operations/${uuid}`),
  recoverOperation: (uuid: string, action: 'retry' | 'rollback') =>
    request<OperationDto>('POST', `/api/operations/${uuid}/recover`, { action }),

  repositories: () => request<{ repositories: RepoDto[] }>('GET', '/api/repositories'),
  repoPaths: (name: string, revision?: string) =>
    request<{ paths: string[] }>(
      'GET',
      `/api/repositories/${encodeURIComponent(name)}/paths${revision ? `?revision=${encodeURIComponent(revision)}` : ''}`,
    ),
  createRepository: (name: string) => {
    const promise = request<OperationDto>('POST', '/api/repositories', {
      name,
    })
    return trackSubmitted(promise)
  },
  repositoryAction: (
    name: string,
    action: 'clone' | 'fetch' | 'pull' | 'push',
    url?: string,
  ) => {
    const promise = request<OperationDto>(
      'POST',
      `/api/repositories/${encodeURIComponent(name)}/${action}`,
      url === undefined ? {} : { url },
    )
    return trackSubmitted(promise)
  },
}
