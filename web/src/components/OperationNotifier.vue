<script setup lang="ts">
// Lives inside the notification provider: watches operations submitted from
// this session and reports their terminal states as notifications.
import { h } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useNotification } from 'naive-ui';
import { api, collectSettledTracked, unauthorized } from '../api/client';
import { translateId } from '../i18n';

const { t } = useI18n();
const router = useRouter();
const notification = useNotification();

let busy = false;
async function check() {
  if (busy || unauthorized.value || document.visibilityState !== 'visible') {
    return;
  }
  busy = true;
  try {
    const { operations } = await api.operations(100);
    const settled = collectSettledTracked(operations);
    for (const operation of settled) {
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
  } catch {
    // Notifications are best-effort; polling views surface real errors.
  } finally {
    busy = false;
  }
}

setInterval(check, 5000);
</script>

<template>
  <span style="display: none" />
</template>
