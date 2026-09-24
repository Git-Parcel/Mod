<script setup lang="ts">
// Lives inside the notification provider: watches operations submitted from
// this session and reports their terminal states as notifications.
import { h, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useNotification } from 'naive-ui';
import { unauthorized } from '../api/client';
import { useOperationsFeed } from '../composables/operationsFeed';
import { collectSettledTracked } from '../composables/trackedOperations';
import { translateId } from '../i18n';

const { t } = useI18n();
const router = useRouter();
const notification = useNotification();
const { operations } = useOperationsFeed();

const notified = new Set<string>();

// The shared feed refreshes itself; watch its data for terminal states of
// operations this session submitted.
watch(operations, (list) => {
  if (unauthorized.value || document.visibilityState !== 'visible') {
    return;
  }
  for (const operation of collectSettledTracked(list)) {
    if (notified.has(operation.operationId)) continue;
    notified.add(operation.operationId);
    const succeeded = operation.state === 'succeeded';
    notification.create({
      title: succeeded ? t('notifications.completed') : t('notifications.failed'),
      content: `${translateId('opKind', operation.kind)} — ${operation.result ?? operation.error ?? ''}`,
      type: succeeded ? 'success' : 'error',
      duration: 8000,
      action: () =>
        h(
          'button',
          {
            class: 'notification-link',
            onClick: () => router.push('/operations'),
          },
          t('common.viewOperations'),
        ),
    });
  }
});
</script>

<template>
  <span style="display: none" />
</template>
