import { type ComputedRef, inject, type InjectionKey, type Ref } from "vue";
import type { SnapshotNodeDto } from "../api/types";

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
