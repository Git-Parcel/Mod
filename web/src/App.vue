<script setup lang="ts">
import { computed, onErrorCaptured, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { darkTheme, dateEnUS, dateZhCN, enUS, zhCN, type GlobalThemeOverrides } from 'naive-ui';
import { captureTokenFromUrl, unauthorized } from './api/client';
import { LOCALES, setLocale, type Locale } from './i18n';

captureTokenFromUrl();

const { t, locale } = useI18n();
const route = useRoute();
const router = useRouter();

const naiveLocale = computed(() => (locale.value === 'zh-CN' ? zhCN : enUS));
const naiveDateLocale = computed(() => (locale.value === 'zh-CN' ? dateZhCN : dateEnUS));

const navItems = computed(() => [
  { label: t('nav.overview'), key: '/' },
  { label: t('nav.parcels'), key: '/parcels' },
  { label: t('nav.operations'), key: '/operations' },
  { label: t('nav.repositories'), key: '/repositories' },
]);

const activeKey = computed(() => '/' + (route.path.split('/')[1] ?? ''));

const currentLocale = computed(() => locale.value as Locale);
const localeOptions = LOCALES.map((value) => ({
  label: value === 'zh-CN' ? '中文' : 'English',
  value,
}));

function onLocaleChange(value: Locale) {
  setLocale(value);
}

// region appearance
type ThemeName = 'light' | 'dark' | 'system';
const THEME_KEY = 'gitparcel-theme';
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

function readStoredTheme(): ThemeName {
  try {
    const value = localStorage.getItem(THEME_KEY);
    if (value === 'light' || value === 'dark' || value === 'system') return value;
  } catch {
    /* 隐私模式等场景读不了就算了 */
  }
  return 'system';
}

const theme = ref<ThemeName>(readStoredTheme());
const resolvedDark = computed(
  () => theme.value === 'dark' || (theme.value === 'system' && prefersDark.matches),
);
const naiveTheme = computed(() => (resolvedDark.value ? darkTheme : null));

function applyTheme() {
  document.documentElement.dataset.theme = resolvedDark.value ? 'dark' : 'light';
}

watch(resolvedDark, applyTheme, { immediate: true });

watch(theme, (value) => {
  try {
    localStorage.setItem(THEME_KEY, value);
  } catch {
    /* 隐私模式等场景存不了就算了 */
  }
});

const onSystemThemeChange = () => {
  if (theme.value === 'system') applyTheme();
};
prefersDark.addEventListener('change', onSystemThemeChange);
onUnmounted(() => prefersDark.removeEventListener('change', onSystemThemeChange));

const themeOptions = computed(() => [
  { label: t('theme.light'), value: 'light' as const, icon: '☀️' },
  { label: t('theme.dark'), value: 'dark' as const, icon: '🌙' },
  { label: t('theme.system'), value: 'system' as const, icon: '🖥️' },
]);

// Modrinth 调色板（web/src/theme.css 同源）：组件库取值在此下发
const lightOverrides: GlobalThemeOverrides = {
  common: {
    fontFamily: 'var(--font)',
    primaryColor: '#00af5c',
    primaryColorHover: '#1fc06c',
    primaryColorPressed: '#04914f',
    primaryColorSuppl: '#00af5c',
    successColor: '#00af5c',
    warningColor: '#e08325',
    errorColor: '#cb2245',
    infoColor: '#686a72',
    bodyColor: '#ebebeb',
    cardColor: '#ffffff',
    modalColor: '#ffffff',
    popoverColor: '#ffffff',
    tableColor: '#ffffff',
    tableHeaderColor: '#f8f8f8',
    textColorBase: '#1a202c',
    textColor1: '#1a202c',
    textColor2: '#2c2e31',
    textColor3: '#484d54',
    placeholderColor: '#83868d',
    borderColor: '#dddddd',
    dividerColor: '#e5e5e8',
    borderRadius: '10px',
    borderRadiusSmall: '8px',
  },
  Card: { borderRadius: '12px', borderColor: '#e5e5e8' },
  Dialog: { borderRadius: '12px' },
  Tag: { borderRadius: '999px' },
  DataTable: {
    borderRadius: '12px',
    borderColor: '#e5e5e8',
    thColor: '#f8f8f8',
    thTextColor: '#484d54',
    thFontWeight: '600',
  },
};

const darkOverrides: GlobalThemeOverrides = {
  common: {
    fontFamily: 'var(--font)',
    primaryColor: '#1bd96a',
    primaryColorHover: '#48e088',
    primaryColorPressed: '#17c05c',
    primaryColorSuppl: '#1bd96a',
    successColor: '#42e686',
    warningColor: '#ffa347',
    errorColor: '#ff496e',
    infoColor: '#9fa4b3',
    bodyColor: '#16181c',
    cardColor: '#27292e',
    modalColor: '#1d1f23',
    popoverColor: '#1d1f23',
    tableColor: '#27292e',
    tableHeaderColor: '#303339',
    textColorBase: '#ffffff',
    textColor1: '#ffffff',
    textColor2: '#b0bac5',
    textColor3: '#96a2b0',
    placeholderColor: '#777b8b',
    borderColor: '#34363c',
    dividerColor: '#2f3136',
    borderRadius: '10px',
    borderRadiusSmall: '8px',
  },
  Card: { borderRadius: '12px', borderColor: '#2f3136' },
  Dialog: { borderRadius: '12px' },
  Tag: { borderRadius: '999px' },
  DataTable: {
    borderRadius: '12px',
    borderColor: '#2f3136',
    thColor: '#303339',
    thTextColor: '#96a2b0',
    thFontWeight: '600',
  },
};

const themeOverrides = computed(() => (resolvedDark.value ? darkOverrides : lightOverrides));
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

// A new route starts scrolled to the top.
watch(
  () => route.fullPath,
  () => window.scrollTo({ top: 0 }),
);
</script>

<template>
  <n-config-provider
    :locale="naiveLocale"
    :date-locale="naiveDateLocale"
    :theme="naiveTheme"
    :theme-overrides="themeOverrides"
  >
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider :max="4">
          <operation-notifier />
          <div class="shell">
            <header class="topbar">
              <div class="topbar-inner">
                <div class="brand">
                  <span class="brand-mark">
                    <svg
                      viewBox="0 0 24 24"
                      width="16"
                      height="16"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linejoin="round"
                    >
                      <path d="M21 8l-9-5-9 5v8l9 5 9-5V8z" />
                      <path d="M3 8l9 5 9-5" />
                      <path d="M12 13v9" />
                    </svg>
                  </span>
                  <span class="brand-name">{{ t('app.title') }}</span>
                </div>

                <nav class="nav">
                  <button
                    v-for="item in navItems"
                    :key="item.key"
                    type="button"
                    class="nav-item"
                    :class="{ active: activeKey === item.key }"
                    @click="router.push(item.key)"
                  >
                    <span>{{ item.label }}</span>
                  </button>
                </nav>

                <div class="actions">
                  <div class="seg" role="group" :aria-label="t('theme.label')">
                    <button
                      v-for="opt in themeOptions"
                      :key="opt.value"
                      type="button"
                      class="seg-item"
                      :class="{ active: theme === opt.value }"
                      :title="opt.label"
                      :aria-label="opt.label"
                      @click="theme = opt.value"
                    >
                      {{ opt.icon }}
                    </button>
                  </div>

                  <n-dropdown
                    trigger="click"
                    :options="localeOptions"
                    @select="onLocaleChange"
                  >
                    <button type="button" class="lang-btn" :aria-label="t('nav.language')">
                      <span>{{ currentLocale === 'zh-CN' ? '中文' : 'English' }}</span>
                      <svg
                        viewBox="0 0 24 24"
                        width="12"
                        height="12"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path d="M6 9l6 6 6-6" />
                      </svg>
                    </button>
                  </n-dropdown>
                </div>
              </div>
            </header>

            <main class="content">
              <n-result
                v-if="unauthorized"
                status="403"
                :title="t('auth.invalid')"
                :description="t('auth.hint')"
                style="margin-top: 6rem"
              />
              <n-result
                v-else-if="renderCrashed"
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
            </main>
          </div>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
.shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶栏：品牌 + 导航 + 偏好控件 */
.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--surface-4);
  border-bottom: 1px solid var(--border-strong);
}
.topbar-inner {
  max-width: 1240px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: -0.01em;
  color: var(--text-1);
  flex-shrink: 0;
}
/* 品牌图标：品牌绿渐变圆角块 */
.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: var(--brand);
  background: linear-gradient(135deg, rgba(0, 175, 92, 0.25) 0%, rgba(29, 217, 106, 0.18) 100%);
}
.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
/* 导航项：胶囊形，悬浮换表面色，选中铺品牌绿软底 */
.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  color: var(--text-2);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}
.nav-item:hover {
  background: var(--surface-2);
}
.nav-item.active {
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 主题分段：iOS 风格胶囊，激活项浮起 */
.seg {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border-radius: var(--radius-round);
  background: var(--surface-2);
}
.seg-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 24px;
  border: none;
  border-radius: var(--radius-round);
  background: transparent;
  font-size: 13px;
  cursor: pointer;
  filter: grayscale(1);
  opacity: 0.65;
}
.seg-item:hover {
  opacity: 1;
}
.seg-item.active {
  background: var(--surface-4);
  box-shadow: var(--shadow-light);
  filter: none;
  opacity: 1;
}

/* 语言下拉触发器：与主题分段同风格的胶囊 */
.lang-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: var(--radius-round);
  background: var(--surface-2);
  color: var(--text-2);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}
.lang-btn:hover {
  color: var(--text-1);
  background: var(--surface-5);
}

/* 内容区：通栏留白 + 居中容器 */
.content {
  flex: 1;
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
}

/* ---- 移动端适配 ---- */
@media (max-width: 720px) {
  .topbar-inner {
    height: auto;
    flex-wrap: wrap;
    padding: 0 12px;
    gap: 0 12px;
  }
  .brand {
    order: 1;
    flex: 1;
    min-width: 0;
  }
  .actions {
    order: 2;
    padding: 8px 0;
  }
  .nav {
    order: 3;
    flex-basis: 100%;
    height: 44px;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .nav::-webkit-scrollbar {
    display: none;
  }
  .nav-item {
    flex-shrink: 0;
    height: 34px;
  }
  .content {
    padding: 12px;
  }
}
@media (max-width: 560px) {
  .brand-name {
    display: none;
  }
  .lang-btn {
    padding: 0 9px;
    gap: 3px;
  }
}
</style>

<style>
.notification-link {
  border: none;
  background: none;
  color: var(--link);
  cursor: pointer;
  padding: 0;
}
</style>
