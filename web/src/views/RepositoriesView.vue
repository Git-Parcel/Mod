<script setup lang="ts">
import { computed, h, ref } from 'vue';
import { useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { useI18n } from 'vue-i18n';

import { api } from '../api/client';
import type { RepoDto } from '../api/types';
import { useApiData } from '../composables/apiData';
import { useErrorToast } from '../composables/errorToast';
import { usePolling } from '../composables/polling';
import { formatTime } from '../utils/format';

const { t } = useI18n();
const message = useMessage();
const run = useErrorToast();

const {
  data: repositories,
  failed,
  error: loadError,
  refresh,
} = useApiData<RepoDto[]>(() => api.repositories().then((r) => r.repositories));
const repoList = computed(() => repositories.value ?? []);

usePolling(refresh, 20000);

async function runAction(name: string, action: 'fetch' | 'pull' | 'push') {
  const operation = await run(() => api.repositoryAction(name, action));
  if (operation) {
    message.success(t('common.operationStarted'));
  }
}

const columns = computed<DataTableColumns<RepoDto>>(() => [
  { title: t('col.repoName'), key: 'name', render: (repo) => h('strong', repo.name) },
  {
    title: t('col.repoType'),
    key: 'type',
    render: (repo) => translateRepoType(repo.type),
  },
  { title: t('col.remoteUrl'), key: 'remoteUrl', render: (repo) => repo.remoteUrl ?? '—' },
  {
    title: t('col.lastSync'),
    key: 'lastSync',
    render: (repo) => (repo.lastSync ? formatTime(repo.lastSync) : '—'),
  },
  {
    title: t('col.actions'),
    key: 'actions',
    render: (repo) =>
      h('span', { class: 'actions' }, [
        actionButton(t('repos.fetch'), () => runAction(repo.name, 'fetch')),
        actionButton(t('repos.pull'), () => runAction(repo.name, 'pull')),
        actionButton(t('repos.push'), () => runAction(repo.name, 'push')),
      ]),
  },
]);

function actionButton(label: string, onClick: () => void) {
  return h(
    'button',
    { class: 'link-button', onClick },
    label,
  );
}

function translateRepoType(type: string): string {
  if (type === 'local') return t('repos.type.local');
  if (type === 'cloned') return t('repos.type.cloned');
  return type;
}

// region create / clone modals
const showCreate = ref(false);
const createName = ref('');
const showClone = ref(false);
const cloneForm = ref({ name: '', url: '' });

async function submitCreate() {
  const operation = await run(() => api.createRepository(createName.value.trim()));
  if (operation) {
    showCreate.value = false;
    createName.value = '';
    message.success(t('common.operationStarted'));
  }
}

async function submitClone() {
  const operation = await run(() =>
    api.repositoryAction(cloneForm.value.name.trim(), 'clone', cloneForm.value.url.trim()),
  );
  if (operation) {
    showClone.value = false;
    cloneForm.value = { name: '', url: '' };
    message.success(t('common.operationStarted'));
  }
}
// endregion
</script>

<template>
  <div>
    <div class="toolbar">
      <h2 style="margin: 0">{{ t('repos.title') }}</h2>
      <span style="flex: 1" />
      <n-button @click="showClone = true">{{ t('repos.clone') }}</n-button>
      <n-button type="primary" @click="showCreate = true">{{ t('repos.create') }}</n-button>
    </div>

    <n-alert v-if="failed && (repositories ?? []).length === 0" type="error" :title="t('common.error')">
      {{ loadError ?? t('apiErrors.network') }}
    </n-alert>
    <n-empty v-else-if="repoList.length === 0" :description="t('repos.none')" style="margin-top: 3rem" />

    <n-data-table
      v-else
      :columns="columns"
      :data="repoList"
      :row-key="(repo: RepoDto) => repo.name"
      :bordered="false"
      size="small"
      style="margin-top: 0.75rem"
    />

    <n-modal
      v-model:show="showCreate"
      preset="card"
      :title="t('repos.create')"
      style="width: 26rem"
    >
      <n-form label-placement="left" label-width="8rem">
        <n-form-item :label="t('repos.createName')">
          <n-input v-model:value="createName" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :disabled="!createName.trim()" @click="submitCreate">
            {{ t('common.create') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="showClone"
      preset="card"
      :title="t('repos.clone')"
      style="width: 30rem"
    >
      <n-form label-placement="left" label-width="8rem">
        <n-form-item :label="t('repos.createName')">
          <n-input v-model:value="cloneForm.name" />
        </n-form-item>
        <n-form-item :label="t('repos.cloneUrl')">
          <n-input v-model:value="cloneForm.url" placeholder="https://…" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showClone = false">{{ t('common.cancel') }}</n-button>
          <n-button
            type="primary"
            :disabled="!cloneForm.name.trim() || !cloneForm.url.trim()"
            @click="submitClone"
          >
            {{ t('repos.clone') }}
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
}
.actions {
  display: inline-flex;
  gap: 0.75rem;
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
