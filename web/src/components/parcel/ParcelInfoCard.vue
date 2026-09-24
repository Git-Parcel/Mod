<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { ParcelDto, Vec3 } from '../../api/types';
import { translateId } from '../../i18n';
import { formatBytes, formatCoord, formatSize } from '../../utils/format';

const props = defineProps<{ parcel: ParcelDto }>();
const { t } = useI18n();

const sync = computed(() => {
  const value = props.parcel;
  if (!value.archiveSync) {
    return { label: t('sync.never'), kind: 'never' as const };
  }
  const same = (a: Vec3, b: Vec3) => a[0] === b[0] && a[1] === b[1] && a[2] === b[2];
  const synced =
    same(value.archiveSync.size, value.sizeParcel) &&
    same(value.archiveSync.anchor, value.anchorParcel);
  return synced
    ? { label: t('sync.synced'), kind: 'synced' as const }
    : { label: t('sync.outOfSync'), kind: 'outOfSync' as const };
});
</script>

<template>
  <n-card size="small" :title="t('detail.info')">
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
        {{ sync.label }}
        <template v-if="parcel.archiveSync">
          · {{ t('detail.repositorySize') }}:
          {{ formatBytes(parcel.archiveSync.repositorySizeBytes) }}
        </template>
      </n-descriptions-item>
    </n-descriptions>
  </n-card>
</template>
