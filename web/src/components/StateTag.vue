<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import type { OperationState } from '../api/types';

const props = defineProps<{ state: OperationState }>();

const { t } = useI18n();

const types: Record<OperationState, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  queued: 'default',
  running: 'info',
  succeeded: 'success',
  failed: 'error',
  canceled: 'warning',
};

const label = computed(() => t(`state.${props.state}`));
</script>

<template>
  <n-tag size="small" :type="types[state]" :bordered="false">{{ label }}</n-tag>
</template>
