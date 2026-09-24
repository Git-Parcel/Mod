import { type ComputedRef, inject, type InjectionKey, type Ref } from "vue";
import type { SnapshotNodeDto } from "../api/types";

/** Tree shape rebuilt from a flat, newest-first page aggregate of nodes. */
export interface SnapshotTreeStructure {
  /** Children grouped by parent id; only parents present in the node set. */
  childrenOf: Map<string, SnapshotNodeDto[]>;
  /** Nodes without a parent in the set — roots of visible subgraphs. */
  roots: SnapshotNodeDto[];
}

/**
 * Rebuilds the snapshot parent/child structure from a flat node list.
 * Nodes whose parent has not been loaded yet surface as roots so partial
 * aggregates still render.
 */
export function buildSnapshotTree(nodes: SnapshotNodeDto[]): SnapshotTreeStructure {
  const ids = new Set(nodes.map((node) => node.id));
  const childrenOf = new Map<string, SnapshotNodeDto[]>();
  const roots: SnapshotNodeDto[] = [];
  for (const node of nodes) {
    if (node.parentId !== null && ids.has(node.parentId)) {
      const list = childrenOf.get(node.parentId) ?? [];
      list.push(node);
      childrenOf.set(node.parentId, list);
    } else {
      roots.push(node);
    }
  }
  return { childrenOf, roots };
}

/** Shared render state passed from SnapshotTree down to its recursive nodes. */
export interface SnapshotTreeApi {
  childrenOf: ComputedRef<Map<string, SnapshotNodeDto[]>>;
  current: ComputedRef<string | null>;
  collapsed: Ref<Set<string>>;
  toggle: (id: string) => void;
}

export const SNAPSHOT_TREE_KEY: InjectionKey<SnapshotTreeApi> = Symbol(
  "snapshot-tree",
);

export function useSnapshotTree(): SnapshotTreeApi {
  const tree = inject(SNAPSHOT_TREE_KEY);
  if (!tree) {
    throw new Error("SnapshotTreeNode must be rendered inside SnapshotTree");
  }
  return tree;
}
