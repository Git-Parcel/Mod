<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useDialog, useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import { ApiError } from '../api/client';
import type {
  OperationDto,
  ParcelDto,
  PlayerDto,
  RepoDto,
  SnapshotNodeDto,
  TreePageDto,
  Vec3,
} from '../api/types';
import { SNAPSHOT_ID_PATTERN } from '../api/types';
import CopyText from '../components/CopyText.vue';
import CoordInput from '../components/CoordInput.vue';
import DimensionTag from '../components/DimensionTag.vue';
import SnapshotTree from '../components/SnapshotTree.vue';
import StateTag from '../components/StateTag.vue';
import { usePolling } from '../composables/polling';
import { translateId } from '../i18n';
import { errorText } from '../utils/errors';
import {
  abbreviate,
  formatBytes,
  formatCoord,
  formatProgress,
  formatSize,
  formatTime,
} from '../utils/format';

const HISTORY_PAGE_LIMIT = 100;
const HISTORY_MAX_NODES = 1000;

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const dialog = useDialog();
const message = useMessage();

const uuid = computed(() => route.params.uuid as string);

const parcel = ref<ParcelDto | null>(null);
const notFound = ref(false);
const recentOperations = ref<OperationDto[]>([]);

async function refresh() {
  try {
    parcel.value = await api.parcel(uuid.value);
    notFound.value = false;
  } catch {
    notFound.value = true;
    return;
  }
  try {
    const { operations } = await api.operations(100);
    recentOperations.value = operations.filter((operation) => operation.target === uuid.value);
  } catch {
    // Secondary data; page content stays usable.
  }
}

usePolling(refresh, 15000);

const syncLabel = computed(() => {
  const value = parcel.value;
  if (!value) return '';
  if (!value.archiveSync) return t('sync.never');
  const same = (a: Vec3, b: Vec3) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2];
  const synced =
    same(value.archiveSync.size, value.sizeParcel) &&
    same(value.archiveSync.anchor, value.anchorParcel);
  return synced ? t('sync.synced') : t('sync.outOfSync');
});

// region configuration form
const form = ref({
  name: '',
  author: '',
  description: '',
});

watch(
  parcel,
  (value) => {
    if (!value) return;
    form.value.name = value.name ?? '';
    form.value.author = value.author ?? '';
    form.value.description = value.description ?? '';
  },
  { immediate: true },
);

async function saveConfig(key: string, value: string | number | boolean) {
  try {
    parcel.value = await api.updateConfig(uuid.value, key, value);
    message.success(t('common.saved'));
  } catch (error) {
    message.error(errorText(error));
  }
}

const saveName = () => saveConfig('meta.name', form.value.name);
const saveAuthor = () => saveConfig('meta.author', form.value.author);
const saveDescription = () => saveConfig('meta.description', form.value.description);
const saveExcludeEntities = (value: boolean) => saveConfig('meta.excludeEntities', value);
const saveShowWireframe = (value: boolean) => saveConfig('visual.showWireframe', value);
const saveShowAnchor = (value: boolean) => saveConfig('visual.showAnchor', value);
const saveSectionSize = (value: number) => saveConfig('content.blocks.sectionSize', value);
// endregion

// region snapshots
const snapshotName = ref('');
const savingSnapshot = ref(false);

/** Schedules a history refresh once a submitted snapshot op had time to finish. */
function scheduleHistoryReload() {
  if (!historyLoaded.value) return;
  window.setTimeout(() => {
    void loadHistory(true);
  }, 8000);
}

async function saveSnapshot() {
  savingSnapshot.value = true;
  try {
    await api.saveParcel(uuid.value, snapshotName.value || undefined);
    message.success(t('common.operationStarted'));
    scheduleHistoryReload();
  } catch (error) {
    message.error(errorText(error));
  } finally {
    savingSnapshot.value = false;
  }
}

const historyNodes = ref<SnapshotNodeDto[]>([]);
const historyCurrent = ref<string | null>(null);
const historyCursor = ref<string | null>(null);
const historyLoading = ref(false);
const historyLoaded = ref(false);
/** True when pagination stopped at the aggregation cap with more pages available. */
const historyHasMore = ref(false);

const HISTORY_ERROR_MAX_RETRIES = 2;

async function loadHistory(reset: boolean) {
  if (historyLoading.value) return;
  historyLoading.value = true;
  try {
    if (reset) {
      historyNodes.value = [];
      historyCursor.value = null;
    }
    let retries = 0;
    let guard = 0;
    do {
      try {
        const page: TreePageDto = await api.history(
          uuid.value,
          HISTORY_PAGE_LIMIT,
          historyCursor.value ?? undefined,
        );
        historyNodes.value.push(...page.nodes);
        historyCurrent.value = page.current;
        historyCursor.value = page.nextCursor;
        retries = 0;
      } catch (error) {
        // A cursor can go stale between page loads (history rewritten by a
        // save); restart pagination from the newest snapshot instead of
        // bothering the user about it.
        if (error instanceof ApiError && error.code === 'stale_cursor' && retries < HISTORY_ERROR_MAX_RETRIES) {
          retries += 1;
          historyNodes.value = [];
          historyCursor.value = null;
          continue;
        }
        throw error;
      }
      guard += 1;
    } while (
      historyCursor.value &&
      historyNodes.value.length < HISTORY_MAX_NODES &&
      guard < 32
    );
    historyHasMore.value = Boolean(historyCursor.value);
    historyLoaded.value = true;
  } catch (error) {
    message.error(errorText(error));
  } finally {
    historyLoading.value = false;
  }
}

const restoreForm = ref<{ snapshotId: string; mode: 'direct' | 'save-first' }>({
  snapshotId: '',
  mode: 'save-first',
});
const restoreIdValid = computed(() =>
  SNAPSHOT_ID_PATTERN.test(restoreForm.value.snapshotId.trim().toLowerCase()),
);

function confirmRestore(snapshotId: string) {
  if (!SNAPSHOT_ID_PATTERN.test(snapshotId)) {
    message.error(t('apiErrors.invalid_value'));
    return;
  }
  dialog.warning({
    title: t('snapshots.restoreConfirmTitle'),
    content: t('snapshots.restoreConfirm', { id: abbreviate(snapshotId) }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await api.restoreParcel(uuid.value, snapshotId, restoreForm.value.mode);
        message.success(t('common.operationStarted'));
        scheduleHistoryReload();
      } catch (error) {
        message.error(errorText(error));
      }
    },
  });
}

function restoreFromInput() {
  confirmRestore(restoreForm.value.snapshotId.trim().toLowerCase());
}

function restoreFromNode(node: SnapshotNodeDto) {
  restoreForm.value.snapshotId = node.id;
  confirmRestore(node.id);
}
// endregion

// region management: resize / delete / teleport / publish
const resizeForm = ref<{ from: Vec3; to: Vec3 }>({ from: [0, 0, 0], to: [0, 0, 0] });

watch(
  parcel,
  (value) => {
    if (!value) return;
    resizeForm.value.from = [...value.bounds.from] as Vec3;
    resizeForm.value.to = [...value.bounds.to] as Vec3;
  },
  { immediate: true },
);

async function submitResize() {
  try {
    parcel.value = await api.resizeParcel(uuid.value, resizeForm.value.from, resizeForm.value.to);
    message.success(t('common.success') + ' — ' + syncLabel.value);
  } catch (error) {
    message.error(errorText(error));
  }
}

function confirmDelete() {
  const value = parcel.value;
  if (!value) return;
  dialog.warning({
    title: t('manage.delete'),
    content: t('manage.deleteConfirm', { name: value.name ?? value.uuid }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await api.deleteParcel(uuid.value);
        message.success(t('common.success'));
        void router.push('/parcels');
      } catch (error) {
        message.error(errorText(error));
      }
    },
  });
}

const players = ref<PlayerDto[]>([]);
const teleportSelection = ref<string[]>([]);

async function loadPlayers() {
  try {
    players.value = (await api.players()).players;
  } catch (error) {
    message.error(errorText(error));
  }
}

async function submitTeleport() {
  if (teleportSelection.value.length === 0) return;
  try {
    const { count } = await api.teleportParcel(uuid.value, teleportSelection.value);
    message.success(t('common.success') + ` (${count})`);
  } catch (error) {
    message.error(errorText(error));
  }
}

const repositories = ref<RepoDto[]>([]);
const publishForm = ref({ repository: '', path: '', message: '' });
const publishNeedsRepository = computed(() => repositories.value.length === 0);

async function loadRepositories() {
  try {
    repositories.value = (await api.repositories()).repositories;
  } catch (error) {
    message.error(errorText(error));
  }
}

async function submitPublish() {
  try {
    await api.publishParcel(
      uuid.value,
      publishForm.value.repository,
      publishForm.value.path,
      publishForm.value.message || undefined,
    );
    message.success(t('common.operationStarted'));
  } catch (error) {
    message.error(errorText(error));
  }
}

watch(
  parcel,
  (value, previous) => {
    if (value && previous === null) {
      void loadPlayers();
      void loadRepositories();
    }
  },
  { immediate: true },
);
// endregion

const playerOptions = computed(() =>
  players.value.map((player) => ({ label: player.name, value: player.uuid })),
);
const repositoryOptions = computed(() =>
  repositories.value.map((repo) => ({ label: repo.name, value: repo.name })),
);
</script>

<template>
  <div>
    <n-spin v-if="!parcel && !notFound" style="margin-top: 3rem" />
    <n-result v-else-if="notFound" status="404" :title="t('detail.notFound')">
      <template #footer>
        <n-button @click="router.push('/parcels')">{{ t('nav.parcels') }}</n-button>
      </template>
    </n-result>

    <template v-else-if="parcel">
      <div class="toolbar">
        <n-button quaternary @click="router.push('/parcels')">←</n-button>
        <h2 style="margin: 0">{{ parcel.name ?? parcel.uuid }}</h2>
        <dimension-tag :dimension="parcel.dimension" />
        <copy-text :value="parcel.uuid" :display="abbreviate(parcel.uuid)" />
        <span class="muted">{{ formatSize(parcel.sizeParcel) }}</span>
        <span style="flex: 1" />
        <span class="sync" :class="parcel.archiveSync ? (syncLabel === t('sync.synced') ? 'sync-synced' : 'sync-outOfSync') : 'sync-never'">
          {{ syncLabel }}
        </span>
      </div>

      <n-card size="small" :title="t('detail.info')" style="margin-top: 1rem">
        <n-descriptions :column="3" size="small" bordered label-placement="left">
          <n-descriptions-item :label="t('detail.bounds')">
            {{ formatCoord(parcel.bounds.from) }} → {{ formatCoord(parcel.bounds.to) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.sizeParcel')">
            {{ formatSize(parcel.sizeParcel) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.sizeWorld')">
            {{ formatSize(parcel.sizeWorld) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.anchorWorld')">
            {{ formatCoord(parcel.anchorWorld) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('col.transform')">
            {{ translateId('rotation', parcel.rotation) }} /
            {{ translateId('mirror', parcel.mirror) }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.dataVersion')">
            {{ parcel.dataVersion }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.contents')">
            {{ parcel.contents.join(', ') || t('common.none') }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.excludeEntities')">
            {{ parcel.excludeEntities ? '✓' : '✗' }}
          </n-descriptions-item>
          <n-descriptions-item :label="t('detail.archiveSync')">
            {{ syncLabel }}
            <template v-if="parcel.archiveSync">
              · {{ t('detail.repositorySize') }}:
              {{ formatBytes(parcel.archiveSync.repositorySizeBytes) }}
            </template>
          </n-descriptions-item>
        </n-descriptions>
      </n-card>

      <n-card size="small" :title="t('detail.config')" style="margin-top: 1rem">
        <p class="muted" style="margin-top: 0">{{ t('config.saveHint') }}</p>
        <n-form label-placement="left" label-width="11rem">
          <n-form-item :label="t('config.meta.name')">
            <div class="inline-form">
              <n-input v-model:value="form.name" maxlength="255" style="width: 16rem" />
              <n-button size="small" @click="saveName">{{ t('common.save') }}</n-button>
            </div>
          </n-form-item>
          <n-form-item :label="t('config.meta.author')">
            <div class="inline-form">
              <n-input v-model:value="form.author" style="width: 16rem" />
              <n-button size="small" @click="saveAuthor">{{ t('common.save') }}</n-button>
            </div>
          </n-form-item>
          <n-form-item :label="t('config.meta.description')">
            <div class="inline-form">
              <n-input v-model:value="form.description" type="textarea" style="width: 16rem" />
              <n-button size="small" @click="saveDescription">{{ t('common.save') }}</n-button>
            </div>
          </n-form-item>
          <n-form-item :label="t('config.meta.excludeEntities')">
            <n-switch
              :value="parcel.excludeEntities"
              @update:value="(v: boolean) => saveExcludeEntities(v)"
            />
          </n-form-item>
          <n-form-item :label="t('config.visual.showWireframe')">
            <n-switch
              :value="parcel.visual.showWireframe"
              @update:value="(v: boolean) => saveShowWireframe(v)"
            />
          </n-form-item>
          <n-form-item :label="t('config.visual.showAnchor')">
            <n-switch
              :value="parcel.visual.showAnchor"
              @update:value="(v: boolean) => saveShowAnchor(v)"
            />
          </n-form-item>
          <n-form-item :label="t('config.content.blocks.sectionSize')">
            <n-radio-group
              :value="parcel.sectionSize ?? 32"
              @update:value="(v: number) => saveSectionSize(v)"
            >
              <n-radio :value="16">16</n-radio>
              <n-radio :value="32">32</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-form>
      </n-card>

      <n-card size="small" :title="t('detail.snapshots')" style="margin-top: 1rem">
        <div class="snapshot-toolbar">
          <n-input
            v-model:value="snapshotName"
            :placeholder="t('snapshots.saveName')"
            style="width: 14rem"
            @keyup.enter="saveSnapshot"
          />
          <n-button type="primary" :loading="savingSnapshot" @click="saveSnapshot">
            {{ t('snapshots.saveSubmit') }}
          </n-button>
          <span class="muted">{{ t('snapshots.saveDefaultHint') }}</span>
          <span style="flex: 1" />
          <n-button v-if="!historyLoaded" @click="loadHistory(true)">
            {{ t('snapshots.loadHistory') }}
          </n-button>
          <n-button v-else :loading="historyLoading" @click="loadHistory(true)">
            {{ t('snapshots.reloadHistory') }}
          </n-button>
        </div>

        <div v-if="historyLoading && historyNodes.length === 0" class="muted" style="margin: 1rem 0">
          {{ t('snapshots.loading') }}
        </div>
        <p v-else-if="historyLoaded && historyNodes.length === 0" class="muted" style="margin: 1rem 0">
          {{ t('snapshots.empty') }}
        </p>
        <snapshot-tree
          v-else-if="historyNodes.length > 0"
          :nodes="historyNodes"
          :current="historyCurrent"
          @restore="restoreFromNode"
        />
        <div v-if="historyHasMore" style="margin-top: 0.5rem">
          <n-button size="small" :loading="historyLoading" @click="loadHistory(false)">
            {{ t('snapshots.loadMore') }} ({{ historyNodes.length }}+)
          </n-button>
        </div>

        <n-divider style="margin: 1rem 0" />
        <n-form label-placement="left" label-width="11rem" inline>
          <n-form-item :label="t('snapshots.restoreId')" style="width: 100%">
            <n-input
              v-model:value="restoreForm.snapshotId"
              placeholder="0123456789abcdef0123456789abcdef01234567"
              style="width: 26rem"
            />
          </n-form-item>
          <n-form-item :label="t('snapshots.restoreMode')">
            <n-radio-group v-model:value="restoreForm.mode">
              <n-radio value="save-first">{{ t('snapshots.mode.save-first') }}</n-radio>
              <n-radio value="direct">{{ t('snapshots.mode.direct') }}</n-radio>
            </n-radio-group>
          </n-form-item>
          <n-form-item :label="' '">
            <n-button
              type="warning"
              :disabled="!restoreIdValid"
              @click="restoreFromInput"
            >
              {{ t('snapshots.restore') }}
            </n-button>
          </n-form-item>
        </n-form>
      </n-card>

      <n-card size="small" :title="t('detail.manage')" style="margin-top: 1rem">
        <n-form label-placement="left" label-width="11rem">
          <n-form-item :label="t('manage.resize')">
            <div>
              <div class="inline-form">
                <coord-input v-model:value="resizeForm.from" />
                <span>→</span>
                <coord-input v-model:value="resizeForm.to" />
                <n-button size="small" @click="submitResize">{{ t('common.save') }}</n-button>
              </div>
              <p class="muted" style="margin: 0.35rem 0 0">{{ t('manage.resizeHint') }}</p>
            </div>
          </n-form-item>

          <n-form-item :label="t('manage.teleport')">
            <div class="inline-form">
              <n-select
                v-model:value="teleportSelection"
                :options="playerOptions"
                multiple
                filterable
                :placeholder="
                  players.length === 0 ? t('manage.teleportEmpty') : t('manage.teleportPlayers')
                "
                style="width: 20rem"
              />
              <n-button
                size="small"
                :disabled="teleportSelection.length === 0"
                @click="submitTeleport"
              >
                {{ t('manage.teleport') }}
              </n-button>
            </div>
          </n-form-item>

          <n-form-item :label="t('manage.publish')">
            <div style="width: 100%">
              <n-alert v-if="publishNeedsRepository" type="warning" style="margin-bottom: 0.5rem">
                {{ t('manage.noRepositories') }}
              </n-alert>
              <div class="inline-form" style="flex-wrap: wrap">
                <n-select
                  v-model:value="publishForm.repository"
                  :options="repositoryOptions"
                  :disabled="publishNeedsRepository"
                  style="width: 12rem"
                />
                <n-input
                  v-model:value="publishForm.path"
                  :placeholder="t('manage.publishPath')"
                  :disabled="publishNeedsRepository"
                  style="width: 14rem"
                />
                <n-input
                  v-model:value="publishForm.message"
                  :placeholder="t('manage.publishMessage')"
                  :disabled="publishNeedsRepository"
                  style="width: 14rem"
                />
                <n-button
                  size="small"
                  type="primary"
                  :disabled="publishNeedsRepository || !publishForm.path"
                  @click="submitPublish"
                >
                  {{ t('common.confirm') }}
                </n-button>
              </div>
              <p class="muted" style="margin: 0.35rem 0 0">{{ t('manage.publishHint') }}</p>
            </div>
          </n-form-item>

          <n-form-item :label="t('manage.delete')">
            <n-button type="error" secondary @click="confirmDelete">
              {{ t('manage.delete') }}
            </n-button>
          </n-form-item>
        </n-form>
      </n-card>

      <n-card
        v-if="recentOperations.length > 0"
        size="small"
        :title="t('detail.recentOperations')"
        style="margin-top: 1rem; margin-bottom: 2rem"
      >
        <div v-for="operation in recentOperations" :key="operation.operationId" class="op-row">
          <state-tag :state="operation.state" />
          <span>{{ translateId('opKind', operation.kind) }}</span>
          <span class="muted">{{ formatProgress(operation) }}</span>
          <span class="muted">{{ formatTime(operation.submittedAt) }}</span>
        </div>
      </n-card>
    </template>
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
.inline-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.op-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.3rem 0;
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
</style>
