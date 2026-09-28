<script setup lang="ts">
import { computed } from 'vue';
import type { OperationDto } from '../api/types';
import { formatProgress } from '../utils/format';

const props = defineProps<{ operation: OperationDto }>();

// Units differ per phase by protocol; only an indeterminate bar or a per-phase
// fraction is shown — never a merged cross-phase percentage.
const showFraction = computed(() => props.operation.total !== null);
const fraction = computed(() =>
  props.operation.total ? Math.min(1, props.operation.completed / props.operation.total) : 0,
);
</script>

<template>
  <div class="progress-cell">
    <span class="phase">{{ operation.phase }}</span>
    <n-progress
      v-if="showFraction"
      type="line"
      :percentage="fraction * 100"
      :show-indicator="false"
      style="max-width: 8rem"
    />
    <span class="count">{{ formatProgress(operation) }}</span>
  </div>
</template>

<style scoped>
.progress-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.phase {
  font-weight: 500;
}
.count {
  color: var(--faint);
  font-variant-numeric: tabular-nums;
}
</style>
