<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { SnapshotNodeDto } from '../api/types';
import { useSnapshotTree } from './snapshotTree';
import { abbreviate, formatTime } from '../utils/format';
import CopyText from './CopyText.vue';

const props = defineProps<{ node: SnapshotNodeDto }>();
const emit = defineEmits<{ (e: 'restore', node: SnapshotNodeDto): void }>();

const { t } = useI18n();
const tree = useSnapshotTree();

const children = computed(() => tree.childrenOf.value.get(props.node.id) ?? []);
const hasChildren = computed(() => children.value.length > 0);
const isCollapsed = computed(() => tree.collapsed.value.has(props.node.id));
const isCurrent = computed(() => tree.current.value === props.node.id);

const sourceKey = computed(() => `snapshots.source.${props.node.source}`);
const sourceLabel = computed(() =>
  t(sourceKey.value) === sourceKey.value ? props.node.source : t(sourceKey.value),
);
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
      <n-tag v-if="isCurrent" size="small" type="success" :bordered="false">
        ★ {{ t('snapshots.current') }}
      </n-tag>
      <copy-text :value="node.id" :display="abbreviate(node.id)" />
      <n-tooltip v-if="node.description" trigger="hover">
        <template #trigger>
          <span class="name">{{ node.name }}</span>
        </template>
        {{ node.description }}
      </n-tooltip>
      <span v-else class="name">{{ node.name }}</span>
      <n-tag size="small" :bordered="false">{{ sourceLabel }}</n-tag>
      <span class="meta">
        {{ node.author }} · {{ formatTime(node.createdAt) }} ·
        {{ t('snapshots.files', { n: node.content.files }) }}
      </span>
      <span class="spacer" />
      <n-button size="tiny" secondary type="warning" @click="emit('restore', node)">
        {{ t('snapshots.restoreTo') }}
      </n-button>
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
  color: #888;
}
.name {
  font-weight: 500;
}
.meta {
  color: #777;
  font-size: 0.85rem;
}
.spacer {
  flex: 1;
}
.children {
  margin-left: 0.9rem;
  border-left: 1px solid #ddd;
  padding-left: 0.5rem;
}
</style>
