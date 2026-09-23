<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { dateEnUS, dateZhCN, enUS, zhCN } from 'naive-ui';
import { unauthorized } from './api/client';
import { LOCALES, setLocale, type Locale } from './i18n';

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
const localeOptions = LOCALES.map((value) => ({ label: value === 'zh-CN' ? '中文' : 'English', value }));

function onLocaleChange(value: Locale) {
  setLocale(value);
}
</script>

<template>
  <n-config-provider :locale="naiveLocale" :date-locale="naiveDateLocale">
    <n-message-provider>
      <n-dialog-provider>
        <n-result
          v-if="unauthorized"
          status="403"
          :title="t('auth.invalid')"
          :description="t('auth.hint')"
          style="margin-top: 6rem"
        />
        <n-layout v-else has-sider style="height: 100vh">
          <n-layout-sider bordered :width="220" content-style="display:flex;flex-direction:column;height:100%">
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
            </div>
          </n-layout-sider>
          <n-layout-content content-style="padding: 1.25rem 1.5rem; height: 100vh; overflow: auto">
            <router-view />
          </n-layout-content>
        </n-layout>
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
