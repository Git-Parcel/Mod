<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { OperationDto } from '../../api/types';
import StateTag from '../StateTag.vue';
import { translateId } from '../../i18n';
import { formatProgress, formatTime } from '../../utils/format';

defineProps<{ operations: OperationDto[] }>();
const { t } = useI18n();
</script>

<template>
  <n-card
    v-if="operations.length > 0"
    size="small"
    :title="t('detail.recentOperations')"
    style="margin-top: 1rem; margin-bottom: 2rem"
  >
    <div v-for="operation in operations" :key="operation.operationId" class="op-row">
      <state-tag :state="operation.state" />
      <span>{{ translateId('opKind', operation.kind) }}</span>
      <span class="muted">{{ formatProgress(operation) }}</span>
      <span class="muted">{{ formatTime(operation.submittedAt) }}</span>
    </div>
  </n-card>
</template>

<style scoped>
.op-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.3rem 0;
}
.muted {
  color: var(--muted);
}
</style>
