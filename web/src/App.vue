<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { captureTokenFromUrl, fetchStatus, hasToken, type Status } from './api';

// Scaffold page: verifies token flow and API wiring; layout and further
// management views are designed separately.
captureTokenFromUrl();

const status = ref<Status | null>(null);
const error = ref<string | null>(null);
const loading = ref(true);

onMounted(async () => {
  try {
    status.value = await fetchStatus();
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
});

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString();
}
</script>

<template>
  <main class="console">
    <h1>Git Parcel 控制台</h1>

    <p v-if="loading">加载中……</p>
    <p v-else-if="error" class="error">
      {{ error }}
      <span v-if="!hasToken()"><br />请从游戏内 <code>/parcel web start</code> 输出的链接进入。</span>
    </p>
    <dl v-else-if="status">
      <dt>模组版本</dt>
      <dd>{{ status.gitparcel.version }}</dd>
      <dt>Minecraft 版本</dt>
      <dd>{{ status.minecraft.name }}（数据版本 {{ status.minecraft.dataVersion }}）</dd>
      <dt>在线玩家</dt>
      <dd>{{ status.onlinePlayers }} / {{ status.maxPlayers }}</dd>
      <dt>Parcel</dt>
      <dd>
        <span v-for="entry in status.parcels" :key="entry.dimension" class="parcel-count">
          {{ entry.dimension }}：{{ entry.count }}
        </span>
        <span v-if="status.parcels.length === 0">无</span>
      </dd>
      <dt>后台操作</dt>
      <dd>{{ status.operations.active }} 个进行中（近期保留 {{ status.operations.retained }} 个）</dd>
      <dt>服务器时间</dt>
      <dd>{{ formatTime(status.serverTime) }}</dd>
    </dl>
  </main>
</template>

<style scoped>
.console {
  max-width: 40rem;
  margin: 2rem auto;
  padding: 0 1rem;
  font-family: system-ui, sans-serif;
}

dl {
  display: grid;
  grid-template-columns: max-content 1fr;
  gap: 0.5rem 1.5rem;
}

dt {
  color: #666;
}

dd {
  margin: 0;
}

.parcel-count:not(:last-child)::after {
  content: '；';
}

.error {
  color: #b00020;
}
</style>
