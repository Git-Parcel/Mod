<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useDialog, useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { ParcelDto, PlayerDto, RepoDto, Vec3 } from '../../api/types';
import { useErrorToast } from '../../composables/errorToast';
import CoordInput from '../CoordInput.vue';

const props = defineProps<{ parcel: ParcelDto }>();
const emit = defineEmits<{
  (e: 'update', parcel: ParcelDto): void;
  (e: 'deleted'): void;
}>();

const { t } = useI18n();
const dialog = useDialog();
const message = useMessage();
const run = useErrorToast();

// region resize
const resizeForm = ref<{ from: Vec3; to: Vec3 }>({ from: [0, 0, 0], to: [0, 0, 0] });

// Track the polled parcel so bounds changed elsewhere (another admin, a
// restore) refresh the form instead of targeting stale coordinates.
watch(
  () => [props.parcel.bounds.from, props.parcel.bounds.to],
  () => {
    resizeForm.value.from = [...props.parcel.bounds.from] as Vec3;
    resizeForm.value.to = [...props.parcel.bounds.to] as Vec3;
  },
  { immediate: true },
);

async function submitResize() {
  const updated = await run(() =>
    api.resizeParcel(props.parcel.uuid, resizeForm.value.from, resizeForm.value.to),
  );
  if (updated) {
    emit('update', updated);
    message.success(t('common.success'));
  }
}
// endregion

// region delete
function confirmDelete() {
  dialog.warning({
    title: t('manage.delete'),
    content: t('manage.deleteConfirm', { name: props.parcel.name ?? props.parcel.uuid }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const deleted = await run(() => api.deleteParcel(props.parcel.uuid));
      if (deleted !== undefined) {
        message.success(t('common.success'));
        emit('deleted');
      }
    },
  });
}
// endregion

// region teleport
const players = ref<PlayerDto[]>([]);
const playersLoaded = ref(false);
const teleportSelection = ref<string[]>([]);

async function loadPlayers() {
  const result = await run(() => api.players());
  if (result) {
    players.value = result.players;
    playersLoaded.value = true;
  }
}

async function submitTeleport() {
  if (teleportSelection.value.length === 0) return;
  const result = await run(() => api.teleportParcel(props.parcel.uuid, teleportSelection.value));
  if (result) {
    message.success(t('common.success') + ` (${result.count})`);
  }
}

const playerOptions = computed(() =>
  players.value.map((player) => ({ label: player.name, value: player.uuid })),
);
// endregion

// region publish
const repositories = ref<RepoDto[]>([]);
const repositoriesLoaded = ref(false);
const publishForm = ref({ repository: '', path: '', message: '' });
const publishNeedsRepository = computed(() => repositoriesLoaded.value && repositories.value.length === 0);

async function loadRepositories() {
  const result = await run(() => api.repositories());
  if (result) {
    repositories.value = result.repositories;
    repositoriesLoaded.value = true;
  }
}

async function submitPublish() {
  const operation = await run(() =>
    api.publishParcel(
      props.parcel.uuid,
      publishForm.value.repository,
      publishForm.value.path,
      publishForm.value.message || undefined,
    ),
  );
  if (operation) {
    message.success(t('common.operationStarted'));
  }
}

const repositoryOptions = computed(() =>
  repositories.value.map((repo) => ({ label: repo.name, value: repo.name })),
);

function loadAuxiliaryData() {
  void loadPlayers();
  void loadRepositories();
}
loadAuxiliaryData();
// endregion
</script>

<template>
  <n-card size="small" :title="t('detail.manage')">
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
              playersLoaded && players.length === 0
                ? t('manage.teleportEmpty')
                : t('manage.teleportPlayers')
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
</template>

<style scoped>
.inline-form {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.muted {
  color: #888;
}
</style>
