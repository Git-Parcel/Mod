<script setup lang="ts">
import { ref } from 'vue';
import { useMessage } from 'naive-ui';
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  value: string;
  display?: string;
}>();

const { t } = useI18n();
const message = useMessage();
const copied = ref(false);

async function copy() {
  try {
    await navigator.clipboard.writeText(props.value);
    copied.value = true;
    message.success(t('common.copied'));
    setTimeout(() => {
      copied.value = false;
    }, 1500);
  } catch {
    message.error(t("common.copyFailed"));
  }
}
</script>

<template>
  <n-tooltip trigger="hover">
    <template #trigger>
      <code class="copy-text" :title="value" @click.stop="copy">{{ display ?? value }}</code>
    </template>
    {{ value }}
  </n-tooltip>
</template>

<style scoped>
.copy-text {
  cursor: pointer;
  font-family: var(--font-mono, monospace);
}
.copy-text:hover {
  text-decoration: underline;
}
</style>
