<script setup lang="ts">
import { computed, ref } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';
import { api } from '../../api/client';
import type { ParcelDto, Vec3 } from '../../api/types';
import { mirrorOptions, rotationOptions, type DimensionOption } from '../../composables/dimensions';
import { useErrorToast } from '../../composables/errorToast';
import { boxSize, formatSize } from '../../utils/format';
import CoordInput from '../CoordInput.vue';

const props = defineProps<{ show: boolean; dimensions: DimensionOption[]; defaultDimension?: string }>();
const emit = defineEmits<{
  (e: 'update:show', value: boolean): void;
  (e: 'created', parcel: ParcelDto): void;
}>();

const { t } = useI18n();
const message = useMessage();
const run = useErrorToast();

const form = ref({
  dimension: props.defaultDimension ?? "minecraft:overworld",
  from: [0, 0, 0] as Vec3,
  to: [0, 0, 0] as Vec3,
  name: "",
  mirror: "none",
  rotation: "none",
});

const previewSize = computed(() => boxSize(form.value.from, form.value.to));
const previewVolume = computed(() => previewSize.value[0] * previewSize.value[1] * previewSize.value[2]);

function close() {
  emit('update:show', false);
}

async function submit() {
  const parcel = await run(() =>
    api.createParcel({
      dimension: form.value.dimension,
      from: form.value.from,
      to: form.value.to,
      name: form.value.name,
      mirror: form.value.mirror,
      rotation: form.value.rotation,
    }),
  );
  if (parcel) {
    message.success(t('common.success'));
    close();
    emit('created', parcel);
  }
}
</script>

<template>
  <n-modal :show="props.show" preset="card" :title="t('create.title')" style="width: 34rem">
    <n-form label-placement="left" label-width="9rem">
      <n-form-item :label="t('create.dimension')">
        <n-select v-model:value="form.dimension" :options="props.dimensions" />
      </n-form-item>
      <n-form-item :label="t('create.from')">
        <coord-input v-model:value="form.from" />
      </n-form-item>
      <n-form-item :label="t('create.to')">
        <coord-input v-model:value="form.to" />
      </n-form-item>
      <n-form-item :label="t('create.name')">
        <n-input v-model:value="form.name" :placeholder="t('create.namePlaceholder')" maxlength="255" />
      </n-form-item>
      <n-form-item :label="t('create.mirror')">
        <n-select v-model:value="form.mirror" :options="mirrorOptions()" />
      </n-form-item>
      <n-form-item :label="t('create.rotation')">
        <n-select v-model:value="form.rotation" :options="rotationOptions()" />
      </n-form-item>
      <n-form-item :label="t('create.preview')">
        <span class="preview">
          {{ t('col.size') }} {{ formatSize(previewSize) }} ·
          {{ t('col.volume') }} {{ previewVolume.toLocaleString() }}
        </span>
      </n-form-item>
    </n-form>
    <template #footer>
      <n-space justify="end">
        <n-button @click="close">{{ t('common.cancel') }}</n-button>
        <n-button type="primary" :disabled="!form.name" @click="submit">
          {{ t('create.submit') }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>
