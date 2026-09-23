<script setup lang="ts">
import { computed, h, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDialog, useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import type { DataTableColumns } from 'naive-ui';
import { api } from '../api/client';
import type { ParcelDto, Vec3 } from '../api/types';
import CopyText from '../components/CopyText.vue';
import CoordInput from '../components/CoordInput.vue';
import DimensionTag from '../components/DimensionTag.vue';
import { usePolling } from '../composables/polling';
import { translateId } from '../i18n';
import { errorText } from '../utils/errors';
import {
  abbreviate,
  formatBounds,
  formatCoord,
  formatSize,
  formatVolume,
} from '../utils/format';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const dialog = useDialog();
const message = useMessage();

const parcels = ref<ParcelDto[]>([]);
const failed = ref(false);
const search = ref('');
const checkedKeys = ref<string[]>([]);
const activeDimension = ref<string>((route.query.dimension as string) ?? '');

async function refresh() {
  try {
    parcels.value = (await api.parcels()).parcels;
    failed.value = false;
  } catch (error) {
    failed.value = true;
    message.error(errorText(error));
  }
}

usePolling(refresh, 15000);

const dimensions = computed(() => {
  const counts = new Map<string, number>();
  for (const parcel of parcels.value) {
    counts.set(parcel.dimension, (counts.get(parcel.dimension) ?? 0) + 1);
  }
  return [...counts.entries()].map(([dimension, count]) => ({ dimension, count }));
});

const visibleParcels = computed(() => {
  const query = search.value.trim().toLowerCase();
  return parcels.value
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
  const same = (a: Vec3, b: Vec3) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2];
  return same(sync.size, parcel.sizeParcel) && same(sync.anchor, parcel.anchorParcel)
    ? 'synced'
    : 'outOfSync';
}

const columns = computed<DataTableColumns<ParcelDto>>(() => [
  {
    type: 'selection',
  },
  {
    title: t('col.name'),
    key: 'name',
    render: (parcel) =>
      h('span', { class: 'parcel-name' }, parcel.name ?? h('i', { class: 'muted' }, '—')),
  },
  {
    title: t('col.uuid'),
    key: 'uuid',
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
    render: (parcel) => formatSize(parcel.sizeParcel),
  },
  {
    title: t('col.volume'),
    key: 'volume',
    render: (parcel) => formatVolume(parcel.sizeParcel).toLocaleString(),
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
          onClick: () => router.push(`/parcels/${parcel.uuid}`),
        },
        t('parcels.detail'),
      ),
  },
]);

// Parcel-local anchor offset in the DTO backs the archive-sync comparison.

function confirmBatchDelete() {
  const count = checkedKeys.value.length;
  if (count === 0) {
    return;
  }
  dialog.warning({
    title: t('parcels.batchDelete'),
    content: t('parcels.batchDeleteConfirm', { count }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        const { count: deleted } = await api.batchDeleteParcels([...checkedKeys.value]);
        message.success(t('common.success') + ` (${deleted})`);
        checkedKeys.value = [];
        await refresh();
      } catch (error) {
        message.error(errorText(error));
      }
    },
  });
}

// region create modal
const showCreate = ref(false);
const createForm = ref({
  dimension: 'minecraft:overworld',
  from: [0, 0, 0] as Vec3,
  to: [0, 0, 0] as Vec3,
  name: '',
  mirror: 'none',
  rotation: 'none',
});

const dimensionOptions = computed(() => {
  const known = new Set(dimensions.value.map((entry) => entry.dimension));
  for (const fallback of ['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end']) {
    known.add(fallback);
  }
  return [...known].map((value) => ({ label: translateId('dims', value), value }));
});

const mirrorOptions = ['none', 'left_right', 'front_back'].map((value) => ({
  label: translateId('mirror', value),
  value,
}));
const rotationOptions = ['none', 'clockwise_90', 'clockwise_180', 'counterclockwise_90'].map(
  (value) => ({ label: translateId('rotation', value), value }),
);

async function submitCreate() {
  try {
    const parcel = await api.createParcel({
      dimension: createForm.value.dimension,
      from: createForm.value.from,
      to: createForm.value.to,
      name: createForm.value.name,
      mirror: createForm.value.mirror,
      rotation: createForm.value.rotation,
    });
    showCreate.value = false;
    message.success(t('common.success'));
    await refresh();
    void router.push(`/parcels/${parcel.uuid}`);
  } catch (error) {
    message.error(errorText(error));
  }
}
// endregion

// region import modal
const showImport = ref(false);
const repositories = ref<Array<{ label: string; value: string }>>([]);
const importForm = ref({
  repository: '',
  revision: '',
  path: '',
  dimension: 'minecraft:overworld',
  at: [0, 0, 0] as Vec3,
  mirror: 'none',
  rotation: 'none',
});
const pathOptions = ref<Array<{ label: string; value: string }>>([]);
const importNeedsRepository = computed(() => repositories.value.length === 0);

async function openImport() {
  showImport.value = true;
  if (repositories.value.length === 0) {
    try {
      const { repositories: repos } = await api.repositories();
      repositories.value = repos.map((repo) => ({ label: repo.name, value: repo.name }));
    } catch (error) {
      message.error(errorText(error));
    }
  }
}

async function loadPathCandidates() {
  const repository = importForm.value.repository;
  if (!repository) {
    return;
  }
  try {
    const { paths } = await api.repoPaths(repository, importForm.value.revision || undefined);
    pathOptions.value = paths.map((path) => ({ label: path, value: path }));
  } catch (error) {
    message.error(errorText(error));
  }
}

async function submitImport() {
  try {
    await api.importParcel({
      repository: importForm.value.repository,
      revision: importForm.value.revision,
      path: importForm.value.path,
      dimension: importForm.value.dimension,
      at: importForm.value.at,
      mirror: importForm.value.mirror,
      rotation: importForm.value.rotation,
    });
    showImport.value = false;
    message.success(t('common.operationStarted'));
  } catch (error) {
    message.error(errorText(error));
  }
}
// endregion
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
      <n-button @click="openImport">{{ t('parcels.import') }}</n-button>
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
      <n-tab name="">{{ t('operations.filterAll') }}</n-tab>
      <n-tab v-for="entry in dimensions" :key="entry.dimension" :name="entry.dimension">
        <dimension-tag :dimension="entry.dimension" /> {{ entry.count }}
      </n-tab>
    </n-tabs>

    <n-alert v-if="failed && parcels.length === 0" type="error" :title="t('common.error')">
      {{ t('apiErrors.network') }}
    </n-alert>

    <n-data-table
      :columns="columns"
      :data="visibleParcels"
      :row-key="(parcel: ParcelDto) => parcel.uuid"
      v-model:checked-row-keys="checkedKeys"
      :bordered="false"
      size="small"
    />

    <n-modal
      v-model:show="showCreate"
      preset="card"
      :title="t('create.title')"
      style="width: 34rem"
    >
      <n-form label-placement="left" label-width="9rem">
        <n-form-item :label="t('create.dimension')">
          <n-select v-model:value="createForm.dimension" :options="dimensionOptions" />
        </n-form-item>
        <n-form-item :label="t('create.from')">
          <coord-input v-model:value="createForm.from" />
        </n-form-item>
        <n-form-item :label="t('create.to')">
          <coord-input v-model:value="createForm.to" />
        </n-form-item>
        <n-form-item :label="t('create.name')">
          <n-input
            v-model:value="createForm.name"
            :placeholder="t('create.namePlaceholder')"
            maxlength="255"
          />
        </n-form-item>
        <n-form-item :label="t('create.mirror')">
          <n-select v-model:value="createForm.mirror" :options="mirrorOptions" />
        </n-form-item>
        <n-form-item :label="t('create.rotation')">
          <n-select v-model:value="createForm.rotation" :options="rotationOptions" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :disabled="!createForm.name" @click="submitCreate">
            {{ t('create.submit') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="showImport"
      preset="card"
      :title="t('import.title')"
      style="width: 36rem"
    >
      <n-alert v-if="importNeedsRepository" type="warning" style="margin-bottom: 0.75rem">
        {{ t('import.needsRepository') }}
      </n-alert>
      <n-form label-placement="left" label-width="11rem">
        <n-form-item :label="t('import.repository')">
          <n-select
            v-model:value="importForm.repository"
            :options="repositories"
            filterable
            :disabled="importNeedsRepository"
          />
        </n-form-item>
        <n-form-item :label="t('import.revision')">
          <n-input v-model:value="importForm.revision" :disabled="importNeedsRepository" />
        </n-form-item>
        <n-form-item :label="t('import.path')">
          <div style="display: flex; gap: 0.5rem; width: 100%">
            <n-select
              v-model:value="importForm.path"
              :options="pathOptions"
              filterable
              tag
              :disabled="importNeedsRepository"
              style="flex: 1"
            />
            <n-button
              size="small"
              :disabled="importNeedsRepository || !importForm.repository"
              @click="loadPathCandidates"
            >
              {{ t('import.loadPaths') }}
            </n-button>
          </div>
        </n-form-item>
        <n-form-item :label="t('create.dimension')">
          <n-select v-model:value="importForm.dimension" :options="dimensionOptions" />
        </n-form-item>
        <n-form-item :label="t('import.at')">
          <coord-input v-model:value="importForm.at" />
        </n-form-item>
        <n-form-item :label="t('create.mirror')">
          <n-select v-model:value="importForm.mirror" :options="mirrorOptions" />
        </n-form-item>
        <n-form-item :label="t('create.rotation')">
          <n-select v-model:value="importForm.rotation" :options="rotationOptions" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showImport = false">{{ t('common.cancel') }}</n-button>
          <n-button
            type="primary"
            :disabled="importNeedsRepository || !importForm.path || !importForm.revision"
            @click="submitImport"
          >
            {{ t('import.submit') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
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
  color: #888;
}
.parcel-name {
  font-weight: 500;
}
.sync-synced {
  color: #18a058;
}
.sync-outOfSync {
  color: #f0a020;
}
.sync-never {
  color: #999;
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
