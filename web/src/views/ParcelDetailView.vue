<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ApiError, api } from '../api/client';
import type { OperationDto, ParcelDto } from '../api/types';
import CopyText from '../components/CopyText.vue';
import DimensionTag from '../components/DimensionTag.vue';
import ParcelConfigCard from '../components/parcel/ParcelConfigCard.vue';
import ParcelInfoCard from '../components/parcel/ParcelInfoCard.vue';
import ParcelManageCard from '../components/parcel/ParcelManageCard.vue';
import ParcelRecentOperations from '../components/parcel/ParcelRecentOperations.vue';
import ParcelSnapshotsCard from '../components/parcel/ParcelSnapshotsCard.vue';
import { usePolling } from '../composables/polling';
import { abbreviate, formatSize } from '../utils/format';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const uuid = computed(() => route.params.uuid as string);

const parcel = ref<ParcelDto | null>(null);
const notFound = ref(false);
const recentOperations = ref<OperationDto[]>([]);

async function refresh() {
  try {
    parcel.value = await api.parcel(uuid.value);
    notFound.value = false;
  } catch (error) {
    if (error instanceof ApiError && error.code === 'not_found') {
      notFound.value = true;
    }
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

function onParcelUpdated(updated: ParcelDto) {
  parcel.value = updated;
}

function onDeleted() {
  void router.push('/parcels');
}
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
        <n-button quaternary aria-label="back" @click="router.push('/parcels')">←</n-button>
        <h2 style="margin: 0">{{ parcel.name ?? parcel.uuid }}</h2>
        <dimension-tag :dimension="parcel.dimension" />
        <copy-text :value="parcel.uuid" :display="abbreviate(parcel.uuid)" />
        <span class="muted">{{ formatSize(parcel.sizeParcel) }}</span>
      </div>

      <parcel-info-card :parcel="parcel" style="margin-top: 1rem" />
      <parcel-config-card :parcel="parcel" style="margin-top: 1rem" @update="onParcelUpdated" />
      <parcel-snapshots-card :parcel-uuid="uuid" style="margin-top: 1rem" />
      <parcel-manage-card
        :parcel="parcel"
        style="margin-top: 1rem"
        @update="onParcelUpdated"
        @deleted="onDeleted"
      />
      <parcel-recent-operations :operations="recentOperations" />
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
</style>
