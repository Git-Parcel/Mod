<script setup lang="ts">
import { computed, h, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDialog, useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import type { DataTableColumns } from 'naive-ui';
import { api } from '../api/client';
import type { ParcelDto } from '../api/types';
import CopyText from '../components/CopyText.vue';
import CreateParcelModal from '../components/parcel/CreateParcelModal.vue';
import DimensionTag from '../components/DimensionTag.vue';
import ImportParcelModal from '../components/parcel/ImportParcelModal.vue';
import { useApiData } from '../composables/apiData';
import { useDimensionOptions } from '../composables/dimensions';
import { useErrorToast } from '../composables/errorToast';
import { usePolling } from '../composables/polling';
import { translateId } from '../i18n';
import {
  abbreviate,
  formatBounds,
  formatCoord,
  formatNumber,
  formatSize,
  formatVolume,
} from '../utils/format';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const dialog = useDialog();
const message = useMessage();
const run = useErrorToast();

const { data: parcels, failed, refresh } = useApiData(() => api.parcels().then((r) => r.parcels));
const search = ref('');
const checkedKeys = ref<string[]>([]);
const activeDimension = ref<string>((route.query.dimension as string) ?? '');

// Keep the address bar in step so dimension views survive reloads and
// back/forward navigation. The route watch makes the tabs follow history
// navigation; the ref watch makes tab clicks write history.
watch(activeDimension, (value) => {
  void router.replace({
    query: value ? { dimension: value } : {},
  });
});
watch(
  () => route.query.dimension,
  (value) => {
    activeDimension.value = (value as string) ?? '';
  },
);

usePolling(refresh, 15000);

const dimensionOptions = useDimensionOptions(parcels);

const dimensions = computed(() => {
  const counts = new Map<string, number>();
  for (const parcel of parcels.value ?? []) {
    counts.set(parcel.dimension, (counts.get(parcel.dimension) ?? 0) + 1);
  }
  return [...counts.entries()].map(([dimension, count]) => ({ dimension, count }));
});

const visibleParcels = computed(() => {
  const query = search.value.trim().toLowerCase();
  return (parcels.value ?? [])
    .filter((parcel) => !activeDimension.value || parcel.dimension === activeDimension.value)
    .filter(
      (parcel) =>
        !query ||
        (parcel.name ?? '').toLowerCase().includes(query) ||
        parcel.uuid.toLowerCase().includes(query),
    );
});

function syncState(parcel: ParcelDto): 'synced' | 'outOfSync' | 'never' {
  if (!parcel.archiveSync) return 'never';
  const sync = parcel.archiveSync;
  const same = (a: number[], b: number[]) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2];
  return same(sync.size, parcel.sizeParcel) && same(sync.anchor, parcel.anchorParcel)
    ? 'synced'
    : 'outOfSync';
}

function openDetail(parcel: ParcelDto) {
  void router.push(`/parcels/${parcel.uuid}`);
}

function rowProps(parcel: ParcelDto) {
  return {
    style: 'cursor: pointer',
    onClick: (event: MouseEvent) => {
      // Row selection checkboxes, action buttons and copy chips keep their own behavior.
      const target = event.target as HTMLElement | null;
      if (target?.closest('.n-checkbox, button, code')) {
        return;
      }
      openDetail(parcel);
    },
  };
}

const columns = computed<DataTableColumns<ParcelDto>>(() => [
  {
    type: 'selection',
  },
  {
    title: t('col.name'),
    key: 'name',
    sorter: (a, b) => (a.name ?? '').localeCompare(b.name ?? ''),
    render: (parcel) =>
      h('span', { class: 'parcel-name' }, parcel.name ?? h('i', { class: 'muted' }, '—')),
  },
  {
    title: t('col.uuid'),
    key: 'uuid',
    width: 110,
    render: (parcel) => h(CopyText, { value: parcel.uuid, display: abbreviate(parcel.uuid) }),
  },
  {
    title: t('col.dimension'),
    key: 'dimension',
    render: (parcel) => h(DimensionTag, { dimension: parcel.dimension }),
  },
  {
    title: t('col.bounds'),
    key: 'bounds',
    render: (parcel) => formatBounds(parcel.bounds.from, parcel.bounds.to),
  },
  {
    title: t('col.size'),
    key: 'size',
    sorter: (a, b) => formatVolume(a.sizeParcel) - formatVolume(b.sizeParcel),
    render: (parcel) => formatSize(parcel.sizeParcel),
  },
  {
    title: t('col.volume'),
    key: 'volume',
    width: 100,
    sorter: (a, b) => formatVolume(a.sizeParcel) - formatVolume(b.sizeParcel),
    render: (parcel) => formatNumber(formatVolume(parcel.sizeParcel)),
  },
  {
    title: t('col.anchor'),
    key: 'anchor',
    render: (parcel) => formatCoord(parcel.anchorWorld),
  },
  {
    title: t('col.transform'),
    key: 'transform',
    render: (parcel) =>
      `${translateId('rotation', parcel.rotation)} / ${translateId('mirror', parcel.mirror)}`,
  },
  {
    title: t('col.author'),
    key: 'author',
    render: (parcel) => parcel.author ?? '—',
  },
  {
    title: t('col.sync'),
    key: 'sync',
    render: (parcel) => {
      const kind = syncState(parcel);
      return h('span', { class: `sync sync-${kind}` }, t(`sync.${kind}`));
    },
  },
  {
    title: t('col.actions'),
    key: 'actions',
    render: (parcel) =>
      h(
        'button',
        {
          class: 'link-button',
          onClick: () => openDetail(parcel),
        },
        t('parcels.detail'),
      ),
  },
]);

function confirmBatchDelete() {
  const count = checkedKeys.value.length;
  if (count === 0) {
    return;
  }
  dialog.warning({
    title: t('parcels.batchDelete'),
    content: t('parcels.batchDeleteConfirm', { count }),
    positiveText: t('parcels.batchDeleteConfirmAction'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const result = await run(() => api.batchDeleteParcels([...checkedKeys.value]));
      if (result) {
        message.success(t('common.success') + ` (${result.count})`);
        checkedKeys.value = [];
        await refresh();
      }
    },
  });
}

const showCreate = ref(false);
const showImport = ref(false);

function onCreated(parcel: ParcelDto) {
  void refresh();
  void router.push(`/parcels/${parcel.uuid}`);
}
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 style="margin: 0">{{ t('parcels.title') }}</h2>
      <span class="muted">{{ t('parcels.total', { count: visibleParcels.length }) }}</span>
      <span style="flex: 1" />
      <n-input
        v-model:value="search"
        :placeholder="t('parcels.search')"
        clearable
        style="width: 14rem"
      />
      <n-button @click="showImport = true">{{ t('parcels.import') }}</n-button>
      <n-button type="primary" @click="showCreate = true">{{ t('parcels.create') }}</n-button>
      <n-button
        type="error"
        secondary
        :disabled="checkedKeys.length === 0"
        @click="confirmBatchDelete"
      >
        {{ t('parcels.batchDelete') }} ({{ checkedKeys.length }})
      </n-button>
    </div>

    <n-tabs
      v-if="dimensions.length > 0"
      type="segment"
      :value="activeDimension"
      style="margin: 0.75rem 0"
      @update:value="(value: string) => (activeDimension = value)"
    >
      <n-tab name="">{{ t('common.all') }}</n-tab>
      <n-tab v-for="entry in dimensions" :key="entry.dimension" :name="entry.dimension">
        <dimension-tag :dimension="entry.dimension" /> {{ entry.count }}
      </n-tab>
    </n-tabs>

    <n-alert v-if="failed && (parcels ?? []).length === 0" type="error" :title="t('common.error')">
      {{ t('apiErrors.network') }}
    </n-alert>

    <n-data-table
      :columns="columns"
      :data="visibleParcels"
      :row-key="(parcel: ParcelDto) => parcel.uuid"
      :row-props="rowProps"
      v-model:checked-row-keys="checkedKeys"
      :pagination="visibleParcels.length > 20 ? { pageSize: 20 } : false"
      :bordered="false"
      size="small"
    />

    <create-parcel-modal
      v-model:show="showCreate"
      :dimensions="dimensionOptions"
      :default-dimension="activeDimension || undefined"
      @created="onCreated"
    />
    <import-parcel-modal
      v-model:show="showImport"
      :dimensions="dimensionOptions"
      @imported="refresh"
    />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.muted {
  color: var(--muted);
}
.parcel-name {
  font-weight: 500;
}
.sync-synced {
  color: var(--success);
}
.sync-outOfSync {
  color: var(--warning);
}
.sync-never {
  color: var(--faint);
}
.link-button {
  border: none;
  background: none;
  color: var(--link);
  cursor: pointer;
  padding: 0;
}
.link-button:hover {
  text-decoration: underline;
}
</style>
