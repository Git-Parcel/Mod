<script setup lang="ts">
import { computed, onErrorCaptured, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { darkTheme, dateEnUS, dateZhCN, enUS, zhCN } from 'naive-ui';
import { captureTokenFromUrl, unauthorized } from './api/client';
import { LOCALES, setLocale, type Locale } from './i18n';

captureTokenFromUrl();

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const naiveLocale = computed(() => (locale.value === 'zh-CN' ? zhCN : enUS));
const naiveDateLocale = computed(() => (locale.value === 'zh-CN' ? dateZhCN : dateEnUS));

const activeKey = computed(() => '/' + (route.path.split('/')[1] ?? ''));

const menuOptions = computed(() => [
  { label: t('nav.overview'), key: '/' },
  { label: t('nav.parcels'), key: '/parcels' },
  { label: t('nav.operations'), key: '/operations' },
  { label: t('nav.repositories'), key: '/repositories' },
]);

function onMenuSelect(key: string) {
  void router.push(key);
}

const currentLocale = computed(() => locale.value as Locale);
const localeOptions = LOCALES.map((value) => ({
  label: value === 'zh-CN' ? '中文' : 'English',
  value,
}));

function onLocaleChange(value: Locale) {
  setLocale(value);
}

// region appearance
const THEME_KEY = 'gitparcel-theme';
type ThemeName = 'light' | 'dark';
const theme = ref<ThemeName>(localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light');
const naiveTheme = computed(() => (theme.value === 'dark' ? darkTheme : null));
const darkMode = computed({
  get: () => theme.value === 'dark',
  set: (value: boolean) => {
    theme.value = value ? 'dark' : 'light';
  },
});
watch(theme, (value) => {
  localStorage.setItem(THEME_KEY, value);
  document.documentElement.dataset.theme = value;
});
document.documentElement.dataset.theme = theme.value;
// endregion

// Render-crash containment: a broken view shows an error card with a way
// back instead of blanking the whole console.
const renderCrashed = ref(false);
const renderError = ref<string | null>(null);
onErrorCaptured((error) => {
  renderCrashed.value = true;
  renderError.value = error instanceof Error ? error.message : String(error);
  return false;
});

function recoverFromCrash() {
  renderCrashed.value = false;
  renderError.value = null;
  void router.push('/');
}

// A new route starts scrolled to the top; the layout content is the scroller.
const contentRef = ref<InstanceType<typeof import('naive-ui')['NLayoutContent']>>();
watch(
  () => route.fullPath,
  () => {
    contentRef.value?.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior });
  },
);
</script>

<template>
  <n-config-provider :locale="naiveLocale" :date-locale="naiveDateLocale" :theme="naiveTheme">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider :max="4">
          <operation-notifier />
          <n-result
            v-if="unauthorized"
            status="403"
            :title="t('auth.invalid')"
            :description="t('auth.hint')"
            style="margin-top: 6rem"
          />
          <n-layout v-else has-sider style="height: 100vh">
            <n-layout-sider
              bordered
              :width="220"
              content-style="display:flex;flex-direction:column;height:100%"
            >
              <div class="brand">
                <span class="brand-title">{{ t('app.title') }}</span>
              </div>
              <n-menu :value="activeKey" :options="menuOptions" @update:value="onMenuSelect" />
              <div class="sider-footer">
                <span>{{ t('nav.language') }}</span>
                <n-select
                  :value="currentLocale"
                  :options="localeOptions"
                  size="small"
                  style="width: 7.5rem"
                  @update:value="onLocaleChange"
                />
                <n-switch v-model:value="darkMode" size="small">
                  <template #checked>🌙</template>
                  <template #unchecked>☀️</template>
                </n-switch>
              </div>
            </n-layout-sider>
            <n-layout-content
              ref="contentRef"
              content-style="padding: 1.25rem 1.5rem; height: 100vh; overflow: auto"
            >
              <n-result
                v-if="renderCrashed"
                status="500"
                :title="t('common.error')"
                :description="renderError ?? ''"
              >
                <template #footer>
                  <n-button type="primary" @click="recoverFromCrash">
                    {{ t('nav.overview') }}
                  </n-button>
                </template>
              </n-result>
              <router-view v-else :key="route.fullPath" />
            </n-layout-content>
          </n-layout>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
.brand {
  padding: 1rem 1.25rem;
  font-weight: 600;
  font-size: 1.05rem;
}
.sider-footer {
  margin-top: auto;
  padding: 1rem 1.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  color: #666;
}
</style>

<style>
.notification-link {
  border: none;
  background: none;
  color: #2080f0;
  cursor: pointer;
  padding: 0;
}
</style>

<style>
/* Theme-aware custom properties for component styles that naive-ui
   variables do not reach. */
:root {
  --muted: #888;
  --faint: #999;
  --link: #2080f0;
  --success: #18a058;
  --warning: #f0a020;
  --line: #ddd;
}

:root[data-theme='dark'] {
  --muted: #9aa2ad;
  --faint: #7d8590;
  --link: #66b2ff;
  --success: #4fc98a;
  --warning: #ffc163;
  --line: #3a3a3f;
}
</style>
