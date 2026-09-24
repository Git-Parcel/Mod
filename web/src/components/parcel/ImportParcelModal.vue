<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { RepoDto, Vec3 } from '../../api/types';
import {
  mirrorOptions,
  rotationOptions,
  type DimensionOption,
} from '../../composables/dimensions';
import { useErrorToast } from '../../composables/errorToast';
import CoordInput from '../CoordInput.vue';

const props = defineProps<{ show: boolean; dimensions: DimensionOption[] }>();
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void;
  (e: 'imported'): void;
}>();

const { t } = useI18n();
const message = useMessage();
const run = useErrorToast();

const repositories = ref<Array<{ label: string; value: string }>>([]);
const repositoriesLoaded = ref(false);
const form = ref({
  repository: '',
  revision: '',
  path: '',
  dimension: 'minecraft:overworld',
  at: [0, 0, 0] as Vec3,
  mirror: 'none',
  rotation: 'none',
});
const pathOptions = ref<Array<{ label: string; value: string }>>([]);
const needsRepository = computed(() => repositoriesLoaded.value && repositories.value.length === 0);

watch(
  () => props.show,
  async (show) => {
    if (show && !repositoriesLoaded.value) {
      const result = await run(() => api.repositories());
      if (result) {
        repositories.value = result.repositories.map((repo: RepoDto) => ({
          label: repo.name,
          value: repo.name,
        }));
        repositoriesLoaded.value = true;
      }
    }
  },
);

async function loadPathCandidates() {
  if (!form.value.repository) return;
  const result = await run(() =>
    api.repoPaths(form.value.repository, form.value.revision || undefined),
  );
  if (result) {
    pathOptions.value = result.paths.map((path) => ({ label: path, value: path }));
  }
}

function close() {
  emit('update:show', false);
}

async function submit() {
  const operation = await run(() =>
    api.importParcel({
      repository: form.value.repository,
      revision: form.value.revision,
      path: form.value.path,
      dimension: form.value.dimension,
      at: form.value.at,
      mirror: form.value.mirror,
      rotation: form.value.rotation,
    }),
  );
  if (operation) {
    message.success(t('common.operationStarted'));
    close();
    emit('imported');
  }
}
</script>

<template>
  <n-modal :show="props.show" preset="card" :title="t('import.title')" style="width: 36rem">
    <n-alert v-if="needsRepository" type="warning" style="margin-bottom: 0.75rem">
      {{ t('import.needsRepository') }}
    </n-alert>
    <n-form label-placement="left" label-width="11rem">
      <n-form-item :label="t('import.repository')">
        <n-select
          v-model:value="form.repository"
          :options="repositories"
          filterable
          :disabled="needsRepository"
        />
      </n-form-item>
      <n-form-item :label="t('import.revision')">
        <n-input v-model:value="form.revision" :disabled="needsRepository" />
      </n-form-item>
      <n-form-item :label="t('import.path')">
        <div style="display: flex; gap: 0.5rem; width: 100%">
          <n-select
            v-model:value="form.path"
            :options="pathOptions"
            filterable
            tag
            :disabled="needsRepository"
            style="flex: 1"
          />
          <n-button
            size="small"
            :disabled="needsRepository || !form.repository"
            @click="loadPathCandidates"
          >
            {{ t('import.loadPaths') }}
          </n-button>
        </div>
      </n-form-item>
      <n-form-item :label="t('create.dimension')">
        <n-select v-model:value="form.dimension" :options="props.dimensions" />
      </n-form-item>
      <n-form-item :label="t('import.at')">
        <coord-input v-model:value="form.at" />
      </n-form-item>
      <n-form-item :label="t('create.mirror')">
        <n-select v-model:value="form.mirror" :options="mirrorOptions()" />
      </n-form-item>
      <n-form-item :label="t('create.rotation')">
        <n-select v-model:value="form.rotation" :options="rotationOptions()" />
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="close">{{ t('common.cancel') }}</n-button>
        <n-button
          type="primary"
          :disabled="needsRepository || !form.path || !form.revision"
          @click="submit"
        >
          {{ t('import.submit') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>
