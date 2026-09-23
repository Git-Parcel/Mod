<script setup lang="ts">
import { computed, provide, ref } from 'vue';
import type { SnapshotNodeDto } from '../api/types';
import SnapshotTreeNode from './SnapshotTreeNode.vue';

const props = defineProps<{
  nodes: SnapshotNodeDto[];
  current: string | null;
}>();

const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const ids = computed(() => new Set(props.nodes.map((node) => node.id)));

const childrenOf = computed(() => {
  const map = new Map<string, SnapshotNodeDto[]>();
  for (const node of props.nodes) {
    // A parent outside the loaded set is rendered as a root once it loads.
    if (node.parentId === null || !ids.value.has(node.parentId)) {
      continue;
    }
    const list = map.get(node.parentId) ?? [];
    list.push(node);
    map.set(node.parentId, list);
  }
  return map;
});

const roots = computed(() =>
  props.nodes.filter((node) => node.parentId === null || !ids.value.has(node.parentId)),
);

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

provide('snapshot-tree', {
  childrenOf,
  current: computed(() => props.current),
  collapsed,
  toggle,
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
