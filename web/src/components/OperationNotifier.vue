<script setup lang="ts">
// Lives inside the notification provider: reports terminal states of
// operations submitted from this session as notifications.
import { h } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useNotification } from "naive-ui";
import { unauthorized } from "../api/client";
import { useOperationsFeed } from "../composables/operationsFeed";
import { onTrackedSettled } from "../composables/trackedOperations";
import { translateId } from "../i18n";

const { t } = useI18n();
const router = useRouter();
const notification = useNotification();
// Starts the shared feed this notifier listens on.
useOperationsFeed();

onTrackedSettled((operation) => {
  if (unauthorized.value) return;
  const succeeded = operation.state === "succeeded";
  notification.create({
    title: succeeded ? t("notifications.completed") : t("notifications.failed"),
    content: `${translateId("opKind", operation.kind)} — ${operation.result ?? operation.error ?? ""}`,
    type: succeeded ? "success" : "error",
    duration: 8000,
    action: () =>
      h(
        "button",
        {
          class: "notification-link",
          onClick: () => router.push("/operations"),
        },
        t("common.viewOperations"),
      ),
  });
});
</script>

<template>
  <span style="display: none" />
</template>
