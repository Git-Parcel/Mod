<script setup lang="ts">
import { computed } from 'vue';
import { translateId } from '../i18n';

const props = defineProps<{ dimension: string }>();

const label = computed(() => translateId('dims', props.dimension));
</script>

<template>
  <n-tag size="small" :bordered="false">{{ label }}</n-tag>
</template>
