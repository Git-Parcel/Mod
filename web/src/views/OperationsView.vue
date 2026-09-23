<script setup lang="ts">
import { computed, h, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import type { DataTableColumns } from 'naive-ui';
import { api } from '../api/client';
import type { OperationDto, OperationState } from '../api/types';
import CopyText from '../components/CopyText.vue';
import ProgressCell from '../components/ProgressCell.vue';
import StateTag from '../components/StateTag.vue';
import { usePolling } from '../composables/polling';
import { translateId } from '../i18n';
import { abbreviate, formatTime, isOperationActive } from '../utils/format';

const { t } = useI18n();

const operations = ref<OperationDto[]>([]);
const failed = ref(false);
const stateFilter = ref<OperationState | ''>('');

const hasActive = computed(() => operations.value.some(isOperationActive));
const pollInterval = computed(() => (hasActive.value ? 3000 : 30000));

async function refresh() {
  try {
    operations.value = (await api.operations(100)).operations;
    failed.value = false;
  } catch {
    failed.value = true;
  }
}

usePolling(refresh, pollInterval);

const visibleOperations = computed(() =>
  stateFilter.value
    ? operations.value.filter((operation) => operation.state === stateFilter.value)
    : operations.value,
);

const stateOptions = (['', 'queued', 'running', 'succeeded', 'failed', 'canceled'] as const).map(
  (value) => ({
    label: value === '' ? t('operations.filterAll') : t(`state.${value}`),
    value,
  }),
);

const columns = computed<DataTableColumns<OperationDto>>(() => [
  {
    type: 'expand',
    renderExpand: (operation) =>
      h('div', { class: 'expand-body' }, [
        detailLine(t('operations.result'), operation.result),
        detailLine(t('operations.error'), operation.error),
        detailLine(
          t('operations.errorCode'),
          operation.errorCode ? translateId('apiErrors', operation.errorCode) : null,
        ),
      ]),
  },
  {
    title: t('col.operationId'),
    key: 'operationId',
    render: (operation) =>
      h(CopyText, {
        value: operation.operationId,
        display: abbreviate(operation.operationId),
      }),
  },
  {
    title: t('col.kind'),
    key: 'kind',
    render: (operation) => translateId('opKind', operation.kind),
  },
  { title: t('col.target'), key: 'target' },
  { title: t('col.owner'), key: 'owner' },
  {
    title: t('col.state'),
    key: 'state',
    render: (operation) => h(StateTag, { state: operation.state }),
  },
  {
    title: t('col.progress'),
    key: 'progress',
    render: (operation) => h(ProgressCell, { operation }),
  },
  {
    title: t('col.submittedAt'),
    key: 'submittedAt',
    render: (operation) => formatTime(operation.submittedAt),
  },
  {
    title: t('col.completedAt'),
    key: 'completedAt',
    render: (operation) => formatTime(operation.completedAt),
  },
]);

function detailLine(label: string, value: string | null) {
  if (!value) {
    return null;
  }
  return h('p', { style: 'margin:0.2rem 0' }, [h('strong', `${label}: `), value]);
}
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 style="margin: 0">{{ t('operations.title') }}</h2>
      <span style="flex: 1" />
      <n-select
        v-model:value="stateFilter"
        :options="[...stateOptions]"
        style="width: 10rem"
      />
      <n-button @click="refresh">{{ t('common.refresh') }}</n-button>
    </div>

    <n-alert v-if="failed && operations.length === 0" type="error" :title="t('common.error')">
      {{ t('apiErrors.network') }}
    </n-alert>

    <n-data-table
      :columns="columns"
      :data="visibleOperations"
      :row-key="(operation: OperationDto) => operation.operationId"
      :bordered="false"
      size="small"
      style="margin-top: 0.75rem"
    />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.expand-body {
  padding: 0.25rem 1rem;
  max-width: 48rem;
}
</style>
