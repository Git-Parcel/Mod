<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { api } from '../api/client';
import type { Status } from '../api/types';
import DimensionTag from '../components/DimensionTag.vue';
import { useApiData } from '../composables/apiData';
import { usePolling } from '../composables/polling';
import { formatTime } from '../utils/format';

const { t } = useI18n();
const router = useRouter();

const { data: status, failed, error, refresh } = useApiData<Status>(() => api.status());
usePolling(refresh, 10000);

const totalParcels = computed(() =>
  status.value ? status.value.parcels.reduce((sum, entry) => sum + entry.count, 0) : 0,
);
</script>

<template>
  <div class="overview">
    <h2>{{ t('nav.overview') }}</h2>
    <n-alert v-if="failed && !status" type="error" :title="t('common.error')" style="margin-bottom: 1rem">
      {{ error ?? t('apiErrors.network') }}
    </n-alert>
    <n-spin v-if="!status" style="margin-top: 3rem" />
    <template v-else>
      <n-grid :cols="4" :x-gap="12">
        <n-gi>
          <n-card size="small">
            <n-statistic :label="t('overview.mcVersion')" :value="status.minecraft.name" />
            <template #footer>{{ t('overview.dataVersion') }}: {{ status.minecraft.dataVersion }}</template>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small">
            <n-statistic :label="t('overview.modVersion')" :value="status.gitparcel.version" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small">
            <n-statistic :label="t('overview.players')" :value="status.onlinePlayers">
              <template #suffix> / {{ status.maxPlayers }}</template>
            </n-statistic>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small">
            <n-statistic :label="t('overview.activeOperations')" :value="status.operations.active">
              <template #suffix>/ {{ status.operations.retained }}</template>
            </n-statistic>
            <template #footer>
              <n-button text type="primary" @click="router.push('/operations')">
                {{ t('common.viewOperations') }}
              </n-button>
            </template>
          </n-card>
        </n-gi>
      </n-grid>

      <n-card size="small" :title="t('overview.parcelsByDimension')" style="margin-top: 1rem">
        <template #header-extra>
          <span class="muted">{{ t('overview.totalParcels') }}: {{ totalParcels }}</span>
        </template>
        <n-space>
          <n-button
            v-for="entry in status.parcels"
            :key="entry.dimension"
            secondary
            size="large"
            @click="router.push({ path: '/parcels', query: { dimension: entry.dimension } })"
          >
            <dimension-tag :dimension="entry.dimension" />
            <span class="count">{{ entry.count }}</span>
          </n-button>
          <span v-if="status.parcels.length === 0" class="muted">{{ t('common.none') }}</span>
        </n-space>
      </n-card>

      <p class="muted" style="margin-top: 1rem">
        {{ t('overview.serverTime') }}: {{ formatTime(status.serverTime) }}
      </p>
    </template>
  </div>
</template>

<style scoped>
.muted {
  color: var(--muted);
}
.count {
  margin-left: 0.5rem;
  font-variant-numeric: tabular-nums;
}
</style>
