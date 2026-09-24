<script setup lang="ts">
import { computed, nextTick, onMounted, provide, ref } from 'vue';
import type { SnapshotNodeDto } from '../api/types';
import { buildSnapshotTree, SNAPSHOT_TREE_KEY } from './snapshotTree';
import SnapshotTreeNode from './SnapshotTreeNode.vue';

const props = defineProps<{
  nodes: SnapshotNodeDto[];
  current: string | null;
}>();

const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const tree = computed(() => buildSnapshotTree(props.nodes));
const childrenOf = computed(() => tree.value.childrenOf);
const roots = computed(() => tree.value.roots);

const collapsed = ref(new Set<string>());

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
// "current path expanded and located" promise.
onMounted(() => {
  void nextTick(() => {
    if (!props.current) return;
    document
      .getElementById(`snapshot-node-${props.current}`)
      ?.scrollIntoView({ block: 'nearest' });
  });
});
</script>

<template>
  <div class="snapshot-tree">
    <snapshot-tree-node
      v-for="root in roots"
      :key="root.id"
      :node="root"
      @restore="(node) => emit('restore', node)"
    />
  </div>
</template>
