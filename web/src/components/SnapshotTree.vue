<script setup lang="ts">
import { computed, nextTick, provide, ref, watch } from 'vue';
import type { SnapshotNodeDto } from '../api/types';
import { buildSnapshotTree, SNAPSHOT_TREE_KEY } from './snapshotTree';
import SnapshotTreeNode from './SnapshotTreeNode.vue';

/** Trees above this size start fully collapsed except the baseline path. */
const COLLAPSE_THRESHOLD = 300;

const props = defineProps<{
  nodes: SnapshotNodeDto[];
  current: string | null;
}>();

const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const tree = computed(() => buildSnapshotTree(props.nodes));
const childrenOf = computed(() => tree.value.childrenOf);
const roots = computed(() => tree.value.roots);

const collapsed = ref(new Set<string>());
let collapseInitialized = false;

// A large history renders thousands of DOM nodes if fully expanded; start
// collapsed and let the user open branches on demand. The current baseline
// stays reachable through the summary line below.
watch(
  () => props.nodes.length,
  (count) => {
    if (collapseInitialized || count <= COLLAPSE_THRESHOLD) return;
    collapseInitialized = true;
    const collapsedIds = new Set<string>();
    for (const id of childrenOf.value.keys()) {
      collapsedIds.add(id);
    }
    collapsed.value = collapsedIds;
  },
);

function toggle(id: string) {
  const next = new Set(collapsed.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  collapsed.value = next;
}

provide(SNAPSHOT_TREE_KEY, {
  childrenOf,
  current: computed(() => props.current),
  collapsed,
  toggle,
});

// Bring the current baseline into view once rendered, per the design's
// "current path expanded and located" promise. Fires after the async page
// aggregate lands (not on mount, when the tree is still empty) and only once.
const located = ref(false);
watch(
  [() => props.nodes.length, () => props.current],
  ([count, current]) => {
    if (located.value || count === 0 || !current) return;
    located.value = true;
    void nextTick(() => {
      document.getElementById(`snapshot-node-${current}`)?.scrollIntoView({ block: 'nearest' });
    });
  },
);
</script>

<template>
  <div class="snapshot-tree">
    <div v-if="current" class="current-line">
      ★ {{ $t('snapshots.current') }}:
      <copy-text :value="current" :display="current.slice(0, 8)" />
    </div>
    <snapshot-tree-node
      v-for="root in roots"
      :key="root.id"
      :node="root"
      @restore="(node) => emit('restore', node)"
    />
  </div>
</template>

<style scoped>
.snapshot-tree {
  overflow-x: auto;
}
.current-line {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.4rem;
  margin-bottom: 0.35rem;
  border: 1px solid rgba(24, 160, 88, 0.4);
  border-radius: 4px;
  color: #18a058;
  font-size: 0.9rem;
}
</style>
