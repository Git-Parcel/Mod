<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useMessage } from 'naive-ui';
import type { SnapshotNodeDto } from '../api/types';
import { buildSnapshotTree } from './snapshotTree';
import { abbreviate, formatTime } from '../utils/format';
import { translateId } from '../i18n';
import CopyText from './CopyText.vue';

/** Trees above this size start fully collapsed except the baseline path. */
const COLLAPSE_THRESHOLD = 300;

const props = defineProps<{
  nodes: SnapshotNodeDto[];
  current: string | null;
}>();

const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const { t } = useI18n();
const message = useMessage();

async function copyId(id: string) {
  try {
    await navigator.clipboard.writeText(id);
    message.success(t('common.copied'));
  } catch {
    message.error(t('common.copyFailed'));
  }
}

const tree = computed(() => buildSnapshotTree(props.nodes));
const childrenOf = computed(() => tree.value.childrenOf);

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
  // Fires on mount too: the initial page aggregate may already exceed the
  // threshold, and only collapsed subtrees keep the DOM small.
  { immediate: true },
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

interface Row {
  node: SnapshotNodeDto;
  depth: number;
  hasChildren: boolean;
}

// Iterative flattening instead of recursive components: render depth stays
// constant no matter how deep the chain runs.
const rows = computed<Row[]>(() => {
  const { childrenOf: children, roots } = tree.value;
  const result: Row[] = [];
  const walk = (list: SnapshotNodeDto[], depth: number) => {
    for (const node of list) {
      const kids = children.get(node.id) ?? [];
      result.push({ node, depth, hasChildren: kids.length > 0 });
      if (kids.length > 0 && !collapsed.value.has(node.id)) {
        walk(kids, depth + 1);
      }
    }
  };
  walk(roots, 0);
  return result;
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
      ★ {{ t('snapshots.current') }}:
      <copy-text :value="current" :display="current.slice(0, 8)" />
    </div>
    <div
      v-for="row in rows"
      :key="row.node.id"
      class="node-row"
      :class="{ current: row.node.id === current }"
      :id="row.node.id === current ? `snapshot-node-${row.node.id}` : undefined"
      :style="{ paddingLeft: `calc(${row.depth} * 1.1rem)` }"
    >
      <span
        v-if="row.hasChildren"
        class="caret"
        role="button"
        tabindex="0"
        :aria-expanded="!collapsed.has(row.node.id)"
        @click="toggle(row.node.id)"
        @keydown.enter="toggle(row.node.id)"
        @keydown.space.prevent="toggle(row.node.id)"
      >
        {{ collapsed.has(row.node.id) ? '▶' : '▼' }}
      </span>
      <span v-else class="caret" aria-hidden="true">·</span>
      <span v-if="row.node.id === current" class="current-mark">★ {{ t('snapshots.current') }}</span>
      <!-- Lightweight copy chip: a per-node tooltip component is too heavy for
           histories with thousands of nodes. -->
      <span
        class="copy-id"
        role="button"
        tabindex="0"
        :aria-label="t('common.copy')"
        :title="row.node.id"
        @click.stop="copyId(row.node.id)"
      >{{ abbreviate(row.node.id) }}</span>
      <span v-if="row.node.description" class="name" :title="row.node.description">{{ row.node.name }}</span>
      <span v-else class="name">{{ row.node.name }}</span>
      <span class="tag" :class="'source-' + row.node.source">{{ translateId('snapshots.source', row.node.source) }}</span>
      <span class="meta">
        {{ row.node.author }} · {{ formatTime(row.node.createdAt) }} ·
        {{ t('snapshots.files', { n: row.node.content.files }) }}
      </span>
      <span class="spacer" />
      <button
        v-if="row.node.id !== current"
        class="restore-button"
        type="button"
        @click.stop="emit('restore', row.node)"
      >
        {{ t('snapshots.restoreTo') }}
      </button>
    </div>
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
  color: var(--success);
  font-size: 0.9rem;
}
.node-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.4rem;
  border-radius: 4px;
  flex-wrap: wrap;
}
.node-row.current {
  background: rgba(24, 160, 88, 0.12);
  outline: 1px solid rgba(24, 160, 88, 0.35);
}
.caret {
  width: 1rem;
  cursor: pointer;
  user-select: none;
  color: var(--muted);
}
.caret[aria-hidden='true'] {
  cursor: default;
}
.caret:focus-visible {
  outline: 1px solid var(--link);
}
.copy-id {
  font-family: monospace;
  cursor: pointer;
}
.copy-id:hover {
  text-decoration: underline;
}
.copy-id:focus-visible {
  outline: 1px solid var(--link);
}
.current-mark {
  color: var(--success);
  font-weight: 600;
  font-size: 0.85rem;
}
.name {
  font-weight: 500;
}
.tag {
  font-size: 0.8rem;
  color: var(--muted);
  border: 1px solid currentColor;
  border-radius: 3px;
  padding: 0 0.3rem;
}
.source-saved {
  color: var(--link);
}
.source-imported {
  color: var(--warning);
}
.meta {
  color: var(--muted);
  font-size: 0.85rem;
}
.spacer {
  flex: 1;
}
.restore-button {
  border: 1px solid var(--warning);
  background: none;
  color: var(--warning);
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.8rem;
  padding: 0.1rem 0.4rem;
}
.restore-button:hover {
  background: rgba(240, 160, 32, 0.12);
}
.restore-button:focus-visible {
  outline: 1px solid var(--link);
}
</style>
