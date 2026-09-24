<script setup lang="ts">
import { computed, h, ref, type VNode } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { api } from '../api/client';
import type { OperationDto, OperationState } from '../api/types';
import CopyText from '../components/CopyText.vue';
import ProgressCell from '../components/ProgressCell.vue';
import StateTag from '../components/StateTag.vue';
import { useOperationsFeed } from '../composables/operationsFeed';
import { useErrorToast } from '../composables/errorToast';
import { translateId } from '../i18n';
import { abbreviate, formatTime } from '../utils/format';

const { t } = useI18n();
const router = useRouter();
const message = useMessage();
const run = useErrorToast();

/** Resumes a failed restore; mirrors /parcel restore recover. */
async function recover(operation: OperationDto, action: 'retry' | 'rollback') {
  const submitted = await run(() => api.recoverOperation(operation.operationId, action));
  if (submitted) {
    message.success(t('common.operationStarted'));
  }
}

/** Parcel-scoped operations carry the parcel UUID as their target. */
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;

const { operations, failed, refresh } = useOperationsFeed();
const stateFilter = ref<OperationState | ''>('');

/** Parcel names for readable targets; names fall back to the raw UUID. */
const parcelNames = ref(new Map<string, string>());
void (async () => {
  try {
    const { parcels: list } = await api.parcels();
    const map = new Map<string, string>();
    for (const parcel of list) {
      map.set(parcel.uuid, parcel.name ?? parcel.uuid);
    }
    parcelNames.value = map;
  } catch {
    // Names are best-effort decoration for the target column.
  }
})();

const visibleOperations = computed(() => {
  const list = operations.value ?? [];
  const filtered = stateFilter.value
    ? list.filter((operation) => operation.state === stateFilter.value)
    : list;
  // Resolve display labels here so later parcel loads re-render the table.
  return filtered.map((operation) => ({
    ...operation,
    targetLabel: UUID_PATTERN.test(operation.target)
      ? (parcelNames.value.get(operation.target) ?? abbreviate(operation.target))
      : operation.target,
    /** Failed restores can be resumed; mirrors /parcel restore recover. */
    recoverable: operation.state === "failed" && operation.kind === "restore_snapshot",
  }));
});

const stateOptions = (['', 'queued', 'running', 'succeeded', 'failed', 'canceled'] as const).map(
  (value) => ({
    label: value === '' ? t('operations.filterAll') : t(`state.${value}`),
    value,
  }),
);

type OperationRow = OperationDto & { targetLabel: string };

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
        detailLine(t('operations.recoverHint'), recoverHint(operation)),
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
  {
    title: t('col.target'),
    key: 'target',
    render: (operation) => {
      if (!UUID_PATTERN.test(operation.target)) {
        return operation.target;
      }
      return h(
        'button',
        {
          class: 'link-button',
          onClick: () => router.push(`/parcels/${operation.target}`),
        },
        (operation as OperationRow).targetLabel,
      );
    },
  },
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
    title: t('col.startedAt'),
    key: 'startedAt',
    render: (operation) => formatTime(operation.startedAt),
  },
  {
    title: t('col.completedAt'),
    key: 'completedAt',
    render: (operation) => formatTime(operation.completedAt),
  },
]);

function detailLine(label: string, value: string | VNode | null) {
  if (!value) {
    return null;
  }
  return h("p", { style: "margin:0.2rem 0" }, [h("strong", `${label}: `), value]);
}

/** Recovery actions for a failed restore, as buttons in the expanded row. */
function recoverHint(operation: OperationDto): VNode | null {
  if (operation.state !== 'failed' || operation.kind !== 'restore_snapshot') {
    return null;
  }
  return h('span', { class: 'recover-actions' }, [
    h(
      'button',
      {
        class: 'link-button',
        onClick: () => recover(operation, 'retry'),
      },
      t('operations.recoverRetry'),
    ),
    h(
      'button',
      {
        class: 'link-button',
        onClick: () => recover(operation, 'rollback'),
      },
      t('operations.recoverRollback'),
    ),
  ]);
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

    <n-alert v-if="failed && (operations ?? []).length === 0" type="error" :title="t('common.error')">
      {{ t('apiErrors.network') }}
    </n-alert>

    <n-data-table
      :columns="columns"
      :data="visibleOperations"
      :row-key="(operation: OperationDto) => operation.operationId"
      :pagination="visibleOperations.length > 20 ? { pageSize: 20 } : false"
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
.recover-actions {
  display: inline-flex;
  gap: 0.75rem;
}
.link-button {
  border: none;
  background: none;
  color: #2080f0;
  cursor: pointer;
  padding: 0;
}
.link-button:hover {
  text-decoration: underline;
}
.link-button {
  border: none;
  background: none;
  color: #2080f0;
  cursor: pointer;
  padding: 0;
}
.link-button:hover {
  text-decoration: underline;
}
</style>
