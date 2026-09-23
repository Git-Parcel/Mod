/** JSON shapes returned by the mod's /api endpoints (camelCase per API contract). */

export interface Status {
  minecraft: { name: string; dataVersion: number };
  gitparcel: { version: string };
  onlinePlayers: number;
  maxPlayers: number;
  parcels: Array<{ dimension: string; count: number }>;
  operations: { active: number; retained: number };
  serverTime: string;
}

export type Vec3 = [number, number, number];

export interface ParcelDto {
  uuid: string;
  dimension: string;
  name: string | null;
  description: string | null;
  author: string | null;
  tags: string[];
  excludeEntities: boolean;
  dataVersion: number;
  contents: string[];
  mirror: 'none' | 'left_right' | 'front_back';
  rotation: 'none' | 'clockwise_90' | 'clockwise_180' | 'counterclockwise_90';
  anchorWorld: Vec3;
  anchorParcel: Vec3;
  bounds: { from: Vec3; to: Vec3 };
  sizeParcel: Vec3;
  sizeWorld: Vec3;
  visual: { showWireframe: boolean; showAnchor: boolean };
  sectionSize: number | null;
  archiveSync: {
    size: Vec3;
    anchor: Vec3;
    repositorySizeBytes: number;
  } | null;
}

export type OperationState = 'queued' | 'running' | 'succeeded' | 'failed' | 'canceled';

export interface OperationDto {
  operationId: string;
  kind: string;
  owner: string;
  target: string;
  state: OperationState;
  phase: string;
  completed: number;
  total: number | null;
  unit: string | null;
  submittedAt: string;
  startedAt: string | null;
  updatedAt: string;
  completedAt: string | null;
  result: string | null;
  error: string | null;
  errorCode: string | null;
}

export type SnapshotSource = 'saved' | 'imported';

export interface SnapshotNodeDto {
  id: string;
  parentId: string | null;
  name: string;
  description: string;
  author: string;
  createdAt: string;
  source: SnapshotSource;
  content: { files: number; bytes: number };
}

export interface TreePageDto {
  parcelUuid: string;
  nodes: SnapshotNodeDto[];
  current: string | null;
  nextCursor: string | null;
  error?: string;
}

export interface RepoDto {
  name: string;
  type: string;
  remoteUrl: string | null;
  lastSync: string | null;
}

export interface PlayerDto {
  uuid: string;
  name: string;
}

export const SNAPSHOT_ID_PATTERN = /^[0-9a-f]{40,128}$/;
