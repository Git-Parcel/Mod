<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue';
import { useDialog, useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { ApiError, api } from '../../api/client';
import type { SnapshotNodeDto, TreePageDto } from '../../api/types';
import { SNAPSHOT_ID_PATTERN } from '../../api/types';
import { useErrorToast } from '../../composables/errorToast';
import { onTrackedSettled } from '../../composables/trackedOperations';
import { errorText } from '../../utils/errors';
import { abbreviate } from '../../utils/format';
import SnapshotTree from '../SnapshotTree.vue';

const props = defineProps<{ parcelUuid: string }>();

const { t } = useI18n();
const dialog = useDialog();
const message = useMessage();
const run = useErrorToast();

const HISTORY_PAGE_LIMIT = 100;
const HISTORY_MAX_NODES = 1000;
const HISTORY_ERROR_MAX_RETRIES = 2;

const snapshotName = ref('');
const savingSnapshot = ref(false);

async function saveSnapshot() {
  savingSnapshot.value = true;
  try {
    const operation = await run(() =>
      api.saveParcel(props.parcelUuid, snapshotName.value || undefined),
    );
    if (operation) {
      message.success(t('common.operationStarted'));
    }
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

// When a snapshot op submitted from this page finishes for this parcel, the
// loaded tree is stale — reload it exactly when the operation settles.
const offSettled = onTrackedSettled((operation) => {
  if (
    historyLoaded.value &&
    operation.target === props.parcelUuid &&
    (operation.kind === 'save_snapshot' || operation.kind.startsWith('restore'))
  ) {
    void loadHistory(true);
  }
});
onUnmounted(offSettled);

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
          props.parcelUuid,
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
        if (
          error instanceof ApiError &&
          error.code === 'stale_cursor' &&
          retries < HISTORY_ERROR_MAX_RETRIES
        ) {
          retries += 1;
          historyNodes.value = [];
          historyCursor.value = null;
          continue;
        }
        throw error;
      }
      guard += 1;
    } while (historyCursor.value && historyNodes.value.length < HISTORY_MAX_NODES && guard < 32);
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
const restorePending = ref(false);

function confirmRestore(snapshotId: string) {
  if (!SNAPSHOT_ID_PATTERN.test(snapshotId)) {
    message.error(t('apiErrors.invalid_value'));
    return;
  }
  if (restorePending.value) {
    return;
  }
  restorePending.value = true;
  dialog.warning({
    title: t('snapshots.restoreConfirmTitle'),
    content: t('snapshots.restoreConfirm', { id: abbreviate(snapshotId) }),
    positiveText: t('snapshots.restoreConfirmAction'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const operation = await run(() =>
        api.restoreParcel(props.parcelUuid, snapshotId, restoreForm.value.mode),
      );
      if (operation) {
        message.success(t('common.operationStarted'));
      }
    },
    onAfterLeave: () => {
      restorePending.value = false;
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
</script>

<template>
  <n-card size="small" :title="t('detail.snapshots')">
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
    <p
      v-else-if="historyLoaded && historyNodes.length === 0"
      class="muted"
      style="margin: 1rem 0"
    >
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

    <n-divider v-if="!historyLoaded || historyNodes.length > 0" style="margin: 1rem 0" />
    <n-form
      v-if="!historyLoaded || historyNodes.length > 0"
      label-placement="left"
      label-width="11rem"
      inline
>
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
          :loading="restorePending"
          :disabled="!restoreIdValid"
          @click="restoreFromInput"
        >
          {{ t('snapshots.restore') }}
        </n-button>
      </n-form-item>
    </n-form>
  </n-card>
</template>

<style scoped>
.snapshot-toolbar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.muted {
  color: var(--muted);
}
</style>
