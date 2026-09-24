<script setup lang="ts">
import { ref, watch } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { ParcelDto } from '../../api/types';
import { useErrorToast } from '../../composables/errorToast';

const props = defineProps<{ parcel: ParcelDto }>();
const emit = defineEmits<{ (e: 'update', parcel: ParcelDto): void }>();

const { t } = useI18n();
const message = useMessage();
const run = useErrorToast();

const form = ref({
  name: '',
  author: '',
  description: '',
});

watch(
  () => props.parcel,
  (value) => {
    form.value.name = value.name ?? '';
    form.value.author = value.author ?? '';
    form.value.description = value.description ?? '';
  },
  { immediate: true },
);

async function saveConfig(key: string, value: string | number | boolean) {
  const updated = await run(() => api.updateConfig(props.parcel.uuid, key, value));
  if (updated) {
    message.success(t('common.saved'));
    emit('update', updated);
  }
}
</script>

<template>
  <n-card size="small" :title="t('detail.config')">
    <p class="muted" style="margin-top: 0">{{ t('config.saveHint') }}</p>
    <n-form label-placement="left" label-width="11rem">
      <n-form-item :label="t('config.meta.name')">
        <div class="inline-form">
          <n-input v-model:value="form.name" maxlength="255" style="width: 16rem" />
          <n-button size="small" @click="saveConfig('meta.name', form.name)">
            {{ t('common.save') }}
          </n-button>
        </div>
      </n-form-item>
      <n-form-item :label="t('config.meta.author')">
        <div class="inline-form">
          <n-input v-model:value="form.author" style="width: 16rem" />
          <n-button size="small" @click="saveConfig('meta.author', form.author)">
            {{ t('common.save') }}
          </n-button>
        </div>
      </n-form-item>
      <n-form-item :label="t('config.meta.description')">
        <div class="inline-form">
          <n-input v-model:value="form.description" type="textarea" style="width: 16rem" />
          <n-button size="small" @click="saveConfig('meta.description', form.description)">
            {{ t('common.save') }}
          </n-button>
        </div>
      </n-form-item>
      <n-form-item :label="t('config.meta.excludeEntities')">
        <n-switch
          :value="parcel.excludeEntities"
          @update:value="(v: boolean) => saveConfig('meta.excludeEntities', v)"
        />
      </n-form-item>
      <n-form-item :label="t('config.visual.showWireframe')">
        <n-switch
          :value="parcel.visual.showWireframe"
          @update:value="(v: boolean) => saveConfig('visual.showWireframe', v)"
        />
      </n-form-item>
      <n-form-item :label="t('config.visual.showAnchor')">
        <n-switch
          :value="parcel.visual.showAnchor"
          @update:value="(v: boolean) => saveConfig('visual.showAnchor', v)"
        />
      </n-form-item>
      <n-form-item :label="t('config.content.blocks.sectionSize')">
        <n-radio-group
          :value="parcel.sectionSize ?? 32"
          @update:value="(v: number) => saveConfig('content.blocks.sectionSize', v)"
        >
          <n-radio :value="16">16</n-radio>
          <n-radio :value="32">32</n-radio>
        </n-radio-group>
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
</style>
