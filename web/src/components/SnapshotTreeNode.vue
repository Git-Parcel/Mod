<script setup lang="ts">
import { computed } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import type { SnapshotNodeDto } from '../api/types';
import { useSnapshotTree } from './snapshotTree';
import { translateId } from '../i18n';
import { abbreviate, formatTime } from '../utils/format';

const props = defineProps<{ node: SnapshotNodeDto }>();
const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const { t } = useI18n();
const message = useMessage();
const tree = useSnapshotTree();

async function copyId() {
  try {
    await navigator.clipboard.writeText(props.node.id);
    message.success(t('common.copied'));
  } catch {
    message.error('Copy failed');
  }
}

const children = computed(() => tree.childrenOf.value.get(props.node.id) ?? []);
const hasChildren = computed(() => children.value.length > 0);
const isCollapsed = computed(() => tree.collapsed.value.has(props.node.id));
const isCurrent = computed(() => tree.current.value === props.node.id);

const sourceLabel = computed(() => translateId('snapshots.source', props.node.source));
</script>

<template>
  <div class="tree-node">
    <div
      class="node-row"
      :class="{ current: isCurrent }"
      :id="isCurrent ? `snapshot-node-${node.id}` : undefined"
    >
      <span class="caret" @click="hasChildren && tree.toggle(node.id)">
        {{ hasChildren ? (isCollapsed ? '▶' : '▼') : '·' }}
      </span>
      <span v-if="isCurrent" class="current-mark">★ {{ t('snapshots.current') }}</span>
      <!-- Lightweight copy chip: a per-node tooltip component is too heavy for
           histories with thousands of nodes. -->
      <span
        class="copy-id"
        :title="node.id"
        @click.stop="copyId"
      >{{ abbreviate(node.id) }}</span>
      <span v-if="node.description" class="name" :title="node.description">{{ node.name }}</span>
      <span v-else class="name">{{ node.name }}</span>
      <span class="tag" :class="'source-' + node.source">{{ sourceLabel }}</span>
      <span class="meta">
        {{ node.author }} · {{ formatTime(node.createdAt) }} ·
        {{ t('snapshots.files', { n: node.content.files }) }}
      </span>
      <span class="spacer" />
      <button
        v-if="!isCurrent"
        class="restore-button"
        type="button"
        @click.stop="emit('restore', node)"
      >
        {{ t('snapshots.restoreTo') }}
      </button>
    </div>
    <div v-if="hasChildren && !isCollapsed" class="children">
      <snapshot-tree-node
        v-for="child in children"
        :key="child.id"
        :node="child"
        @restore="(target) => emit('restore', target)"
      />
    </div>
  </div>
</template>

<style scoped>
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
.name {
  font-weight: 500;
}
.meta {
  color: var(--muted);
  font-size: 0.85rem;
}
.spacer {
  flex: 1;
}
.copy-id {
  font-family: monospace;
  cursor: pointer;
}
.copy-id:hover {
  text-decoration: underline;
}
.current-mark {
  color: var(--success);
  font-weight: 600;
  font-size: 0.85rem;
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
.children {
  margin-left: 0.9rem;
  border-left: 1px solid var(--line);
  padding-left: 0.5rem;
}
</style>
