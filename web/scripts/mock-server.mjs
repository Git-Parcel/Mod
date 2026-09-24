// Mock of the mod's /api surface for frontend development and manual testing
// without a running game. Serves the built bundle from dist/ with the same
// SPA fallback and token semantics as the in-game console.
//
//   npm run build
//   npm run mock          # http://127.0.0.1:5640/?token=<MOCK_TOKEN>
//
// State is in-memory and mutating endpoints actually mutate it, so UI edits
// survive polling just like against the real server.
import { createServer } from 'node:http'
import { existsSync, readFileSync, statSync } from 'node:fs'
import { extname, join, normalize, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { randomUUID } from 'node:crypto'

const PORT = Number(process.env.MOCK_PORT ?? 5640)
const MOCK_TOKEN = 'feedfacefeedfacefeedfacefeedface'

const here = fileURLToPath(new URL('.', import.meta.url))
const distDir = resolve(here, '..', 'dist')

// region fixture state
function parcel(id, overrides = {}) {
  return {
    uuid: id,
    dimension: 'minecraft:overworld',
    name: null,
    description: '',
    author: 'Leawind',
    tags: [],
    excludeEntities: true,
    dataVersion: 4440,
    contents: ['gitparcel:block', 'gitparcel:entity'],
    mirror: 'none',
    rotation: 'none',
    anchorWorld: [100, 64, 200],
    anchorParcel: [0, 0, 0],
    bounds: { from: [100, 60, 200], to: [131, 79, 231] },
    sizeParcel: [32, 20, 32],
    sizeWorld: [32, 20, 32],
    visual: { showWireframe: true, showAnchor: true },
    sectionSize: 32,
    archiveSync: null,
    ...overrides,
  }
}

const P_MAIN = '3f0eab4f-9a2b-4c1d-8e3f-a1b2c3d4e5f1'
const P_TOWER = '77c1e2a9-0b3d-4e5f-8a9b-1c2d3e4f5a6b'
const P_EMPTY = 'cc00ff11-2233-4455-8677-8899aabbccdd'
const P_LONG = 'dddd4444-5555-4666-8777-888899990000'
const P_HUGE = 'eeee5555-6666-4777-9888-99990000aaaa'

const parcels = [
  parcel(P_MAIN, {
    name: 'Main House',
    description: 'The primary base building.',
    archiveSync: {
      size: [32, 20, 32],
      anchor: [0, 0, 0],
      repositorySizeBytes: 48211394,
    },
  }),
  parcel(P_TOWER, {
    name: 'Wizard Tower',
    dimension: 'minecraft:the_nether',
    rotation: 'clockwise_90',
    mirror: 'left_right',
    anchorWorld: [-50, 40, -70],
    anchorParcel: [0, 0, 0],
    bounds: { from: [-62, 30, -86], to: [-30, 70, -54] },
    sizeParcel: [33, 41, 33],
    sizeWorld: [33, 41, 33],
    archiveSync: {
      size: [30, 40, 30],
      anchor: [0, 0, 0],
      repositorySizeBytes: 1200455,
    },
  }),
  parcel(P_HUGE, {
    name: 'Monument (huge history)',
  }),
  parcel(P_LONG, {
    name: 'Long History',
    archiveSync: {
      size: [16, 6, 16],
      anchor: [0, 0, 0],
      repositorySizeBytes: 500000,
    },
  }),
  parcel(P_EMPTY, {
    name: 'Empty Yard',
    anchorWorld: [0, 64, 0],
    bounds: { from: [0, 60, 0], to: [15, 65, 15] },
    sizeParcel: [16, 6, 16],
    sizeWorld: [16, 6, 16],
  }),
]

let counter = 0
function snapshotId() {
  counter += 1
  return (counter.toString(16).padStart(6, '0') + 'a'.repeat(34)).slice(0, 40)
}

function node(id, parentId, name, minutesAgo, source = 'saved') {
  const created = new Date(Date.now() - minutesAgo * 60000).toISOString()
  return {
    id,
    parentId,
    name,
    description: '',
    author: 'Leawind',
    createdAt: created,
    source,
    content: {
      files: 120 + Math.floor(Math.random() * 400),
      bytes: 900000 + Math.floor(Math.random() * 4000000),
    },
  }
}

/** Branching history for the main parcel: root -> base -> secondFloor -> (roof | roofAlt). */
const P_MAIN_SNAPSHOT = snapshotId()
const P_BASE = snapshotId()
const P_SECOND = snapshotId()
const P_ROOF = snapshotId()
const P_ROOF_ALT = snapshotId()

const historyByParcel = new Map([
  [
    P_MAIN,
    [
      node(P_MAIN_SNAPSHOT, null, 'Initial version', 600),
      node(P_BASE, P_MAIN_SNAPSHOT, 'Walls and floor', 480),
      node(P_SECOND, P_BASE, 'Add second floor', 300),
      node(P_ROOF, P_SECOND, 'Gabled roof', 120),
      node(P_ROOF_ALT, P_SECOND, 'Alternative roof', 60, 'imported'),
    ],
  ],
  [P_EMPTY, []],
])
const currentByParcel = new Map([[P_MAIN, P_ROOF]])

/** Beyond the 1000-node aggregation cap, so the load-more path is reachable. */
{
  const huge = [node(snapshotId(), null, 'Genesis', 9000)]
  for (let i = 1; i < 1200; i++) {
    huge.push(node(snapshotId(), huge[i - 1].id, `Phase ${i}`, 9000 - i * 7))
  }
  historyByParcel.set(P_HUGE, huge)
  currentByParcel.set(P_HUGE, huge[huge.length - 1].id)
}

/** A long linear history so pagination aggregation and the load-more button
 * get exercised for real (130 nodes > the 1000-node cap is unnecessary; any
 * count above HISTORY_PAGE_LIMIT splits into multiple pages). */
{
  const long = [node(snapshotId(), null, 'Genesis', 3000)]
  for (let i = 1; i < 130; i++) {
    long.push(
      node(snapshotId(), long[i - 1].id, `Revision ${i}`, 3000 - i * 20),
    )
  }
  historyByParcel.set(P_LONG, long)
  currentByParcel.set(P_LONG, long[long.length - 1].id)
}

const repositories = new Map([
  ['community-pack', { type: 'local', remoteUrl: null, lastSync: null }],
  [
    'origin-mirror',
    {
      type: 'cloned',
      remoteUrl: 'https://git.example.com/mc/parcel-pack.git',
      lastSync: new Date(Date.now() - 7200000).toISOString(),
    },
  ],
])

const repoPaths = [
  'builds/main-house',
  'builds/wizard-tower',
  'farms/iron-farm',
]

const players = [
  { uuid: 'aaaa1111-2222-4333-8444-555566667777', name: 'Leawind' },
  { uuid: 'bbbb2222-3333-4444-8555-666677778888', name: 'Steve' },
  { uuid: 'cccc3333-4444-4555-8666-777788889999', name: 'Alex' },
]

const operations = []
function addOperation(kind, target, extra = {}) {
  const now = new Date().toISOString()
  const operation = {
    operationId: randomUUID(),
    kind,
    owner: 'web-console',
    target,
    state: 'queued',
    phase: 'queued',
    completed: 0,
    total: null,
    unit: null,
    submittedAt: now,
    startedAt: null,
    updatedAt: now,
    completedAt: null,
    result: null,
    error: null,
    errorCode: null,
    ...extra,
  }
  operations.unshift(operation)
  if (operations.length > 100) { operations.pop() }
  scheduleProgress(operation)
  return operation
}

/** Simulates queued -> running (progressing) -> terminal for freshly submitted ops. */
function scheduleProgress(operation) {
  setTimeout(() => {
    if (operation.state !== 'queued') { return }
    operation.state = 'running'
    operation.startedAt = new Date().toISOString()
    operation.phase = 'capturing'
    operation.total = 240
    operation.unit = 'sections'
    const timer = setInterval(() => {
      operation.completed = Math.min(240, operation.completed + 60)
      operation.updatedAt = new Date().toISOString()
      if (operation.completed >= 240) {
        clearInterval(timer)
        operation.state = 'succeeded'
        operation.phase = 'done'
        operation.completedAt = operation.updatedAt
        operation.result = operation.kind === 'save_snapshot'
            || operation.kind === 'restore_snapshot'
          ? snapshotId()
          : operation.kind === 'import_snapshot'
          ? randomUUID()
          : 'OK'
      }
    }, 1200)
  }, 800)
}

function seedOperations() {
  addOperation('save_snapshot', P_MAIN, {
    state: 'succeeded',
    phase: 'done',
    completed: 240,
    total: 240,
    unit: 'sections',
    startedAt: new Date(Date.now() - 3600000).toISOString(),
    completedAt: new Date(Date.now() - 3500000).toISOString(),
    result: snapshotId(),
  })
  addOperation('restore_snapshot', P_TOWER, {
    state: 'failed',
    phase: 'decoding',
    completed: 90,
    total: 240,
    unit: 'sections',
    startedAt: new Date(Date.now() - 7200000).toISOString(),
    completedAt: new Date(Date.now() - 7100000).toISOString(),
    error: 'Simulated failure for UI testing',
    errorCode: 'INTERNAL',
  })
  addOperation('push', 'origin-mirror', {
    state: 'canceled',
    phase: 'pushing',
    completed: 0,
    unit: 'references',
    startedAt: new Date(Date.now() - 10800000).toISOString(),
    completedAt: new Date(Date.now() - 10700000).toISOString(),
  })
}
seedOperations()
// endregion

// region helpers
function json(response, status, payload) {
  const body = JSON.stringify(payload)
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-cache',
  })
  response.end(body)
}

function errorJson(response, status, code) {
  json(response, status, { error: code })
}

function readBody(request) {
  return new Promise((resolvePromise) => {
    const chunks = []
    request.on('data', (chunk) => chunks.push(chunk))
    request.on(
      'end',
      () => resolvePromise(Buffer.concat(chunks).toString('utf-8')),
    )
  })
}

function authorized(url, request) {
  if (url.searchParams.get('token') === MOCK_TOKEN) { return true }
  const header = request.headers.authorization ?? ''
  return header === `Bearer ${MOCK_TOKEN}`
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
}

function serveStatic(response, pathname) {
  let relative = decodeURIComponent(pathname).replace(/^\/+/, '')
  if (relative === '') { relative = 'index.html' }
  const target = normalize(join(distDir, relative))
  if (!target.startsWith(distDir)) {
    response.writeHead(400)
    response.end('Invalid path')
    return
  }
  let file = target
  if (!existsSync(file) || statSync(file).isDirectory()) {
    file = join(distDir, 'index.html') // SPA fallback
  }
  if (!existsSync(file)) {
    response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' })
    response.end('dist/ is missing — run `npm run build` first.')
    return
  }
  response.writeHead(200, {
    'Content-Type': MIME[extname(file)] ?? 'application/octet-stream',
    'Cache-Control': 'no-cache',
  })
  response.end(readFileSync(file))
}
// endregion

const server = createServer(async (request, response) => {
  const url = new URL(request.url, `http://127.0.0.1:${PORT}`)
  const path = url.pathname
  const method = request.method ?? 'GET'

  if (!path.startsWith('/api')) {
    serveStatic(response, path)
    return
  }
  if (!authorized(url, request)) {
    errorJson(response, 401, 'unauthorized')
    return
  }

  const body = method === 'GET' || method === 'DELETE' ? {} : JSON.parse((await readBody(request)) || '{}')
  await route(response, method, path, url, body)
})

async function route(response, method, path, url, body) {
  if (method === 'GET' && path === '/api/status') {
    const byDimension = new Map()
    for (const parcel of parcels) {
      byDimension.set(
        parcel.dimension,
        (byDimension.get(parcel.dimension) ?? 0) + 1,
      )
    }
    const recent = operations.slice(0, 100)
    return json(response, 200, {
      minecraft: { name: '26.3', dataVersion: 4440 },
      gitparcel: { version: '0.1.0-alpha+mock' },
      onlinePlayers: players.length,
      maxPlayers: 20,
      parcels: [...byDimension].map(([dimension, count]) => ({
        dimension,
        count,
      })),
      operations: {
        active: recent.filter((operation) => ['queued', 'running'].includes(operation.state)).length,
        retained: recent.length,
      },
      serverTime: new Date().toISOString(),
    })
  }

  if (method === 'GET' && path === '/api/parcels') {
    const dimension = url.searchParams.get('dimension')
    const list = parcels.filter((parcel) => !dimension || parcel.dimension === dimension)
    return json(response, 200, { parcels: list })
  }

  if (method === 'POST' && path === '/api/parcels') {
    const name = body.name ?? ''
    if (!/^[\p{L}\p{N}\p{P} ]{1,255}$/u.test(name) || name.includes('  ')) {
      return errorJson(response, 400, 'invalid_name')
    }
    const size = [0, 1, 2].map((axis) => Math.abs(body.to[axis] - body.from[axis]) + 1)
    const created = parcel(randomUUID(), {
      name,
      dimension: body.dimension ?? 'minecraft:overworld',
      mirror: body.mirror ?? 'none',
      rotation: body.rotation ?? 'none',
      anchorWorld: [...body.from],
      bounds: { from: [...body.from], to: [...body.to] },
      sizeParcel: size,
      sizeWorld: size,
    })
    parcels.push(created)
    historyByParcel.set(created.uuid, [])
    return json(response, 201, created)
  }

  let match = path.match(/^\/api\/parcels\/([^/]+)\/config$/)
  if (method === 'POST' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    if (!parcel) { return errorJson(response, 404, 'not_found') }
    const { key, value } = body
    switch (key) {
      case 'meta.name':
        if (!/^[\p{L}\p{N}\p{P} ]{1,255}$/u.test(value)) {
          return errorJson(response, 400, 'invalid_name')
        }
        parcel.name = value
        break
      case 'meta.author':
        parcel.author = value
        break
      case 'meta.description':
        parcel.description = value
        break
      case 'meta.excludeEntities':
        parcel.excludeEntities = value
        break
      case 'visual.showWireframe':
        parcel.visual.showWireframe = value
        break
      case 'visual.showAnchor':
        parcel.visual.showAnchor = value
        break
      case 'content.blocks.sectionSize':
        if (value !== 16 && value !== 32) {
          return errorJson(response, 400, 'invalid_value')
        }
        parcel.sectionSize = value
        break
      default:
        return errorJson(response, 400, 'invalid_value')
    }
    return json(response, 200, parcel)
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/resize$/)
  if (method === 'POST' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    if (!parcel) { return errorJson(response, 404, 'not_found') }
    const size = [0, 1, 2].map((axis) => Math.abs(body.to[axis] - body.from[axis]) + 1)
    parcel.bounds = { from: [...body.from], to: [...body.to] }
    parcel.sizeParcel = size
    parcel.sizeWorld = size
    return json(response, 200, parcel)
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/save$/)
  if (method === 'POST' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    if (!parcel) { return errorJson(response, 404, 'not_found') }
    return json(response, 202, addOperation('save_snapshot', parcel.uuid))
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/restore$/)
  if (method === 'POST' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    if (!parcel) { return errorJson(response, 404, 'not_found') }
    if (!/^[0-9a-f]{40,128}$/.test(body.snapshotId ?? '')) {
      return errorJson(response, 400, 'invalid_value')
    }
    return json(response, 202, addOperation('restore_snapshot', parcel.uuid))
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/history$/)
  if (method === 'GET' && match) {
    const nodes = historyByParcel.get(match[1]) ?? null
    if (nodes === null) {
      const exists = parcels.some((parcel) => parcel.uuid === match[1])
      if (!exists) { return errorJson(response, 404, 'not_found') }
      return json(response, 200, {
        parcelUuid: match[1],
        nodes: [],
        current: null,
        nextCursor: null,
      })
    }
    const limit = Math.min(100, Number(url.searchParams.get('limit') ?? 20))
    const cursor = url.searchParams.get('cursor')
    const startIndex = cursor ? nodes.findIndex((entry) => entry.id === cursor) + 1 : 0
    if (cursor && startIndex === 0) {
      return errorJson(response, 400, 'stale_cursor')
    }
    const page = nodes.slice(startIndex, startIndex + limit)
    const nextCursor = startIndex + limit < nodes.length ? page[page.length - 1].id : null
    return json(response, 200, {
      parcelUuid: match[1],
      nodes: page,
      current: currentByParcel.get(match[1]) ?? nodes[0]?.id ?? null,
      nextCursor,
    })
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/teleport$/)
  if (method === 'POST' && match) {
    const exists = parcels.some((parcel) => parcel.uuid === match[1])
    if (!exists) { return errorJson(response, 404, 'not_found') }
    return json(response, 200, { count: (body.players ?? []).length })
  }

  match = path.match(/^\/api\/parcels\/([^/]+)\/publish$/)
  if (method === 'POST' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    if (!parcel) { return errorJson(response, 404, 'not_found') }
    if (!repositories.has(body.repository)) {
      return errorJson(response, 400, 'invalid_value')
    }
    return json(
      response,
      202,
      addOperation('publish_snapshot', `${body.repository}:${body.path}`),
    )
  }

  if (method === 'POST' && path === '/api/parcels/batch-delete') {
    const requested = new Set(body.uuids ?? [])
    let count = 0
    for (let index = parcels.length - 1; index >= 0; index--) {
      if (requested.has(parcels[index].uuid)) {
        parcels.splice(index, 1)
        count += 1
      }
    }
    return json(response, 200, { count })
  }

  match = path.match(/^\/api\/parcels\/([^/]+)$/)
  if (method === 'GET' && match) {
    const parcel = parcels.find((candidate) => candidate.uuid === match[1])
    return parcel ? json(response, 200, parcel) : errorJson(response, 404, 'not_found')
  }
  if (method === 'DELETE' && match) {
    const index = parcels.findIndex((candidate) => candidate.uuid === match[1])
    if (index < 0) { return errorJson(response, 404, 'not_found') }
    parcels.splice(index, 1)
    return json(response, 204, null)
  }

  if (method === 'POST' && path === '/api/import') {
    if (!repositories.has(body.repository)) {
      return errorJson(response, 400, 'invalid_value')
    }
    return json(
      response,
      202,
      addOperation(
        'import_snapshot',
        `${body.repository}@${body.revision}:${body.path}`,
      ),
    )
  }

  if (method === 'GET' && path === '/api/players') {
    return json(response, 200, { players })
  }

  if (method === 'GET' && path === '/api/operations') {
    const limit = Math.min(100, Number(url.searchParams.get('limit') ?? 50))
    return json(response, 200, { operations: operations.slice(0, limit) })
  }

  match = path.match(/^\/api\/operations\/([^/]+)$/)
  if (method === 'GET' && match) {
    const operation = operations.find((candidate) => candidate.operationId === match[1])
    return operation ? json(response, 200, operation) : errorJson(response, 404, 'not_found')
  }

  match = path.match(/^\/api\/operations\/([^/]+)\/recover$/)
  if (method === 'POST' && match) {
    const operation = operations.find((candidate) => candidate.operationId === match[1])
    if (!operation) { return errorJson(response, 404, 'not_found') }
    return json(response, 202, addOperation('retry_restore', operation.target))
  }

  if (method === 'GET' && path === '/api/repositories') {
    return json(response, 200, {
      repositories: [...repositories].map(([name, info]) => ({
        name,
        ...info,
      })),
    })
  }

  match = path.match(/^\/api\/repositories\/([^/]+)\/paths$/)
  if (method === 'GET' && match) {
    if (!repositories.has(match[1])) {
      return errorJson(response, 404, 'not_found')
    }
    return json(response, 200, { paths: repoPaths })
  }

  if (method === 'POST' && path === '/api/repositories') {
    const name = (body.name ?? '').trim()
    if (!name || repositories.has(name)) {
      return errorJson(response, 400, 'invalid_value')
    }
    repositories.set(name, { type: 'local', remoteUrl: null, lastSync: null })
    return json(response, 202, addOperation('create', name))
  }

  match = path.match(/^\/api\/repositories\/([^/]+)\/(clone|fetch|pull|push)$/)
  if (method === 'POST' && match) {
    const [, name, action] = match
    if (action === 'clone') {
      if (!/^https:\/\/.+/.test(body.url ?? '')) {
        return errorJson(response, 400, 'invalid_value')
      }
      repositories.set(name, {
        type: 'cloned',
        remoteUrl: body.url,
        lastSync: new Date().toISOString(),
      })
    }
    return json(response, 202, addOperation(action, name))
  }

  errorJson(response, 404, 'not_found')
}

server.listen(PORT, '127.0.0.1', () => {
  console.log(
    `gitparcel mock console: http://127.0.0.1:${PORT}/?token=${MOCK_TOKEN}`,
  )
  console.log(`serving dist/ from ${distDir}`)
})
