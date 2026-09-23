<script setup lang="ts">
import type { Vec3 } from '../api/types';

const props = defineProps<{ value: Vec3 }>();
const emit = defineEmits<{ (e: 'update:value', value: Vec3): void }>();

function setAxis(axis: number, raw: number | null) {
  const next = [...props.value] as Vec3;
  next[axis] = raw ?? 0;
  emit('update:value', next);
}
</script>

<template>
  <span class="coord-input">
    <n-input-number
      :value="value[0]"
      :show-button="false"
      placeholder="x"
      @update:value="(v: number | null) => setAxis(0, v)"
    />
    <n-input-number
      :value="value[1]"
      :show-button="false"
      placeholder="y"
      @update:value="(v: number | null) => setAxis(1, v)"
    />
    <n-input-number
      :value="value[2]"
      :show-button="false"
      placeholder="z"
      @update:value="(v: number | null) => setAxis(2, v)"
    />
  </span>
</template>

<style scoped>
.coord-input {
  display: inline-flex;
  gap: 0.25rem;
}
.coord-input .n-input-number {
  width: 6rem;
}
</style>
